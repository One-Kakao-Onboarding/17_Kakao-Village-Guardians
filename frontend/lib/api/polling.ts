import { messageApi } from './services'
import type { Message, ChatRoomUpdate } from './types'

export type MessageCallback = (messages: Message[]) => void
export type UpdatesCallback = (updates: ChatRoomUpdate[]) => void
export type ErrorCallback = (error: Error) => void

class PollingService {
  private intervals: Map<string, NodeJS.Timeout> = new Map()
  private lastMessageIds: Map<string, string> = new Map()
  private lastUpdateTime: string | null = null
  private globalUpdateInterval: NodeJS.Timeout | null = null

  /**
   * Start polling for new messages in a specific chat room
   * @param roomId - The chat room ID to poll
   * @param callback - Called when new messages are received
   * @param errorCallback - Called when an error occurs
   * @param intervalMs - Polling interval in milliseconds (default: 3000)
   */
  startRoomPolling(
    roomId: string,
    callback: MessageCallback,
    errorCallback?: ErrorCallback,
    intervalMs: number = 3000
  ) {
    // Clear existing interval if any
    this.stopRoomPolling(roomId)

    const poll = async () => {
      try {
        const lastMessageId = this.lastMessageIds.get(roomId)
        const messages = await messageApi.pollMessages(roomId, {
          lastMessageId,
        })

        if (messages.length > 0) {
          callback(messages)
          // Update last message ID
          const latestMessage = messages[messages.length - 1]
          this.lastMessageIds.set(roomId, latestMessage.id)
        }
      } catch (error) {
        if (errorCallback) {
          errorCallback(error as Error)
        } else {
          console.error('Polling error:', error)
        }
      }
    }

    // Initial poll
    poll()

    // Set up interval
    const interval = setInterval(poll, intervalMs)
    this.intervals.set(roomId, interval)
  }

  /**
   * Stop polling for a specific chat room
   */
  stopRoomPolling(roomId: string) {
    const interval = this.intervals.get(roomId)
    if (interval) {
      clearInterval(interval)
      this.intervals.delete(roomId)
    }
    this.lastMessageIds.delete(roomId)
  }

  /**
   * Start polling for updates across all chat rooms
   * @param callback - Called when updates are received
   * @param errorCallback - Called when an error occurs
   * @param intervalMs - Polling interval in milliseconds (default: 5000)
   */
  startGlobalPolling(
    callback: UpdatesCallback,
    errorCallback?: ErrorCallback,
    intervalMs: number = 5000
  ) {
    // Clear existing interval if any
    this.stopGlobalPolling()

    const poll = async () => {
      try {
        const response = await messageApi.getUpdates({
          since: this.lastUpdateTime || undefined,
        })

        if (response.updates.length > 0) {
          callback(response.updates)
        }

        // Update last update time
        this.lastUpdateTime = response.timestamp
      } catch (error) {
        if (errorCallback) {
          errorCallback(error as Error)
        } else {
          console.error('Global polling error:', error)
        }
      }
    }

    // Initial poll
    poll()

    // Set up interval
    this.globalUpdateInterval = setInterval(poll, intervalMs)
  }

  /**
   * Stop global polling
   */
  stopGlobalPolling() {
    if (this.globalUpdateInterval) {
      clearInterval(this.globalUpdateInterval)
      this.globalUpdateInterval = null
    }
    this.lastUpdateTime = null
  }

  /**
   * Stop all polling
   */
  stopAll() {
    // Stop all room polling
    this.intervals.forEach((interval) => clearInterval(interval))
    this.intervals.clear()
    this.lastMessageIds.clear()

    // Stop global polling
    this.stopGlobalPolling()
  }

  /**
   * Check if polling is active for a specific room
   */
  isPolling(roomId: string): boolean {
    return this.intervals.has(roomId)
  }

  /**
   * Check if global polling is active
   */
  isGlobalPolling(): boolean {
    return this.globalUpdateInterval !== null
  }
}

// Singleton instance
export const pollingService = new PollingService()
