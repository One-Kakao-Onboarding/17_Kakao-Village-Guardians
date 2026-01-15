#!/bin/bash

echo "=== Persona Talk Database Migration ==="
echo ""
echo "This script will:"
echo "1. Drop all existing tables"
echo "2. Create new schema"
echo "3. Insert sample data"
echo ""
read -p "Continue? (y/n) " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]
then
    echo "Migration cancelled."
    exit 1
fi

echo ""
echo "Running V1__create_persona_talk_schema.sql..."
psql -h localhost -p 5432 -U postgres -d onboarding -f src/main/resources/db/migration/V1__create_persona_talk_schema.sql

if [ $? -eq 0 ]; then
    echo "✓ Schema created successfully"
else
    echo "✗ Schema creation failed"
    exit 1
fi

echo ""
echo "Running V2__seed_initial_data.sql..."
psql -h localhost -p 5432 -U postgres -d onboarding -f src/main/resources/db/migration/V2__seed_initial_data.sql

if [ $? -eq 0 ]; then
    echo "✓ Sample data inserted successfully"
else
    echo "✗ Sample data insertion failed"
    exit 1
fi

echo ""
echo "=== Migration Complete ==="
echo ""
echo "Verifying tables..."
psql -h localhost -p 5432 -U postgres -d onboarding -c "\dt hackerton.*"

echo ""
echo "Sample user count:"
psql -h localhost -p 5432 -U postgres -d onboarding -c "SELECT COUNT(*) FROM hackerton.users;"
