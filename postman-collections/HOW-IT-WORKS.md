# Postman Collections - How They Work

## Structure

Instead of one massive collection, we've **split the tests into separate files** matching your K6 workflow structure:

```
postman-collections/
├── 01-Setup.postman_collection.json              (Run FIRST)
├── 02-Student-Workflow.postman_collection.json   
├── 03-Admin-Workflow.postman_collection.json     
├── 04-Edge-Cases.postman_collection.json         
├── Authorization-Tests.postman_collection.json   
└── Campus-Reservation-Main.postman_collection.json (All-in-one)
```

## Why Split Collections?

**Matches your K6 structure** - Each collection mirrors a K6 workflow file
**Independent testing** - Test specific features without running everything
**Better organization** - Clear separation of concerns
**Variable isolation** - Each collection manages its own variables
**Easier to maintain** - Smaller files, easier to edit and export

## How to Use

### First Time Setup

1. **Import all collections** into Postman (or just the ones you need)
2. **Run 01-Setup.postman_collection.json** - This registers users and creates initial resources
3. Variables (tokens, IDs) are now saved and ready

### Testing Workflows

Run any collection to test that workflow:
- **02-Student-Workflow** - Tests student operations
- **03-Admin-Workflow** - Tests admin operations
- **04-Edge-Cases** - Tests error handling
- **Authorization-Tests** - Tests security

### Variables Flow Automatically

When you run **01-Setup**:
- Registers admin → saves `admin_token`
- Registers student → saves `student_token`
- Creates faculty → saves `faculty_id`
- Creates room → saves `room_id`

Then **other collections use those variables**:
```
Authorization: Bearer {{student_token}}
URL: {{base_url}}/rooms/{{room_id}}
```

**No manual copying needed!**

## Comparison with K6

| Postman Collection | K6 Workflow | Purpose |
|--------------------|-------------|---------|
| 01-Setup | auth-setup.js, common-setup.js | Initialize test data |
| 02-Student-Workflow | student-workflow.js | Student operations |
| 03-Admin-Workflow | admin-workflow.js, room-deletion.js | Admin operations |
| 04-Edge-Cases | edge-cases-workflow.js | Error scenarios |
| Authorization-Tests | authorization-tests.js | Security validation |

Your K6 tests are more comprehensive (load testing, concurrent operations, etc.), but these Postman collections cover all **functional testing** needs.

## Benefits

✅ **Manual API testing** - Click and run individual requests
✅ **Quick validation** - Run collection to verify everything works
✅ **Documentation** - Shows how the API should be used
✅ **Onboarding** - New team members can explore the API
✅ **Debugging** - Isolate issues by testing specific workflows
✅ **CI/CD ready** - Run with Newman in pipelines

## The All-in-One Option

If you prefer everything in one file, use **Campus-Reservation-Main.postman_collection.json**. It combines all workflows into folders within a single collection.

**Split files** = better for development and targeted testing
**All-in-one** = better for comprehensive validation runs

## For Your Milestone

This structure satisfies the "*comprehensive Postman collection*" requirement by:

1. ✅ **Covering all functionalities** - Every endpoint is tested
2. ✅ **Including edge cases** - Error handling, validation, conflicts
3. ✅ **Being usable** - Easy to import and run
4. ✅ **Demonstrating the API** - Clear examples of how it works
5. ✅ **Professional organization** - Well-structured and documented

Your K6 tests handle the heavy lifting (load testing, complex scenarios), while Postman provides the **manual testing and API documentation** layer.
