# HangOut Backend - Spring Boot + MongoDB

## Overview

This is the backend API for the HangOut restaurant application, built with Spring Boot and MongoDB.

## Technology Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Data MongoDB**
- **Spring Security** (BCrypt password encoding)
- **JWT Authentication**
- **Jakarta Bean Validation**
- **Lombok**
- **Maven**

## Project Structure

```
backend/
├── src/main/java/com/hangout/
│   ├── HangOutApplication.java         # Main application class
│   ├── config/
│   │   ├── CorsConfig.java             # CORS configuration
│   │   ├── SecurityConfig.java         # Security configuration
│   │   └── DataSeeder.java             # Database seed data loader
│   ├── controller/                      # REST Controllers
│   │   ├── AuthController.java
│   │   ├── RestaurantController.java
│   │   ├── ReservationController.java
│   │   ├── ReviewController.java
│   │   └── UserController.java
│   ├── service/                         # Business logic layer
│   │   ├── AuthService.java
│   │   ├── RestaurantService.java
│   │   ├── ReservationService.java
│   │   ├── ReviewService.java
│   │   ├── NotificationService.java
│   │   └── JwtService.java
│   ├── repository/                      # MongoDB repositories
│   │   ├── UserRepository.java
│   │   ├── RestaurantRepository.java
│   │   ├── ReservationRepository.java
│   │   ├── ReviewRepository.java
│   │   ├── MenuItemRepository.java
│   │   ├── EventRepository.java
│   │   ├── HangOutRepository.java
│   │   ├── GameResultRepository.java
│   │   ├── NotificationRepository.java
│   │   └── PointTransactionRepository.java
│   ├── model/                           # MongoDB documents
│   │   ├── User.java
│   │   ├── Restaurant.java
│   │   ├── Reservation.java
│   │   ├── Review.java
│   │   ├── MenuItem.java
│   │   ├── Order.java
│   │   ├── Event.java
│   │   ├── HangOut.java
│   │   ├── GameResult.java
│   │   ├── Notification.java
│   │   └── PointTransaction.java
│   ├── dto/                             # Data Transfer Objects
│   │   ├── AuthRequest.java
│   │   ├── AuthResponse.java
│   │   ├── RegisterRequest.java
│   │   ├── ReviewRequest.java
│   │   ├── ReviewResponse.java
│   │   ├── ReservationRequest.java
│   │   ├── GamePlayRequest.java
│   │   └── UserDto.java
│   └── exception/                       # Exception handling
│       ├── GlobalExceptionHandler.java
│       ├── ResourceNotFoundException.java
│       └── BadRequestException.java
├── src/main/resources/
│   └── application.properties
├── pom.xml
└── README.md
```

## MongoDB Collections

The application uses the following MongoDB collections:

- `users` - User accounts and profiles
- `restaurants` - Restaurant information
- `menu_items` - Restaurant menu items
- `reservations` - Table reservations
- `orders` - Pre-orders
- `reviews` - Customer reviews
- `events` - Restaurant events
- `hangouts` - Social dining groups
- `game_results` - Loyalty game results
- `point_transactions` - Loyalty points history
- `notifications` - User notifications

## Setup Instructions

### Prerequisites

1. **Java 17** or higher
2. **Maven 3.6+**
3. **MongoDB Atlas account** (or local MongoDB)

### Step 1: Configure MongoDB

1. Create a `.env` file in the backend directory (copy from `.env.example`):

```bash
cp .env.example .env
```

2. Update the MongoDB URI in `.env`:

```properties
MONGODB_URI=mongodb+srv://<username>:<password>@cluster0.yxenbsq.mongodb.net/?appName=Cluster0
```

**IMPORTANT:** Replace `<username>` and `<password>` with your actual MongoDB credentials.

### Step 2: Set Environment Variables

**Windows (PowerShell):**
```powershell
$env:MONGODB_URI="your-mongodb-uri-here"
$env:JWT_SECRET="your-secret-key-at-least-256-bits"
```

**Linux/Mac:**
```bash
export MONGODB_URI="your-mongodb-uri-here"
export JWT_SECRET="your-secret-key-at-least-256-bits"
```

### Step 3: Build the Project

```bash
cd backend
mvn clean install
```

### Step 4: Run the Application

```bash
mvn spring-boot:run
```

The server will start on `http://localhost:8080`

## API Endpoints

### Authentication

- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user

### Users

- `GET /api/users/{id}` - Get user by ID
- `GET /api/users/{userId}/reservations` - Get user reservations

### Restaurants

- `GET /api/restaurants` - Get all restaurants
- `GET /api/restaurants/{id}` - Get restaurant by ID
- `GET /api/restaurants/cuisine/{cuisine}` - Get restaurants by cuisine
- `POST /api/restaurants` - Create restaurant
- `PUT /api/restaurants/{id}` - Update restaurant
- `DELETE /api/restaurants/{id}` - Delete restaurant

### Reservations

- `GET /api/reservations/{id}` - Get reservation by ID
- `POST /api/reservations?userId={userId}` - Create reservation
- `DELETE /api/reservations/{id}` - Cancel reservation

### Reviews (Complete Implementation)

- `GET /api/reviews` - Get all reviews
- `GET /api/reviews/{id}` - Get review by ID
- `GET /api/reviews/restaurant/{restaurantId}` - Get restaurant reviews
- `POST /api/reviews/restaurant/{restaurantId}?userId={userId}` - Create review
- `PUT /api/reviews/{reviewId}/helpful?userId={userId}` - Mark review as helpful
- `DELETE /api/reviews/{reviewId}?userId={userId}` - Delete review

**Review Flow:**
1. User submits review via Flutter app
2. POST request to `/api/reviews/restaurant/{restaurantId}`
3. Backend validates review data (rating, comment length)
4. Review saved to MongoDB
5. Restaurant rating automatically recalculated
6. User stats updated (totalReviews incremented)
7. Review returned to Flutter app
8. Flutter UI refreshes to show new review

## Database Seeding

The application automatically seeds the database with sample data on first run:

- 3 users (including demo account)
- 3 restaurants
- Menu items
- 2 reviews
- 1 event
- 1 hangout

**Demo Account:**
- Email: `demo@example.com`
- Password: `123456`

To disable seeding, set in `application.properties`:
```properties
app.seed-data.enabled=false
```

## Testing the Backend

### 1. Health Check

```bash
curl http://localhost:8080/api/restaurants
```

### 2. Register User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "password": "password123"
  }'
```

### 3. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@example.com",
    "password": "123456"
  }'
```

### 4. Create Review

```bash
curl -X POST "http://localhost:8080/api/reviews/restaurant/{restaurantId}?userId={userId}" \
  -H "Content-Type: application/json" \
  -d '{
    "rating": 5.0,
    "title": "Amazing food!",
    "comment": "Best restaurant in town. Highly recommended!"
  }'
```

### 5. Get Restaurant Reviews

```bash
curl http://localhost:8080/api/reviews/restaurant/{restaurantId}
```

## Important Features

### 1. Restaurant Rating Calculation

The backend is the **source of truth** for restaurant ratings:

- Ratings are calculated from published reviews
- Automatically recalculated when reviews are added/deleted
- Formula: Average of all published review ratings
- Stored in `restaurants.rating` field

### 2. Validation

All requests are validated using Jakarta Bean Validation:

- Email format validation
- Password minimum length (6 characters)
- Review comment minimum length (10 characters)
- Rating range (1-5)
- Required fields

### 3. Error Handling

Global exception handler provides consistent error responses:

- 400: Bad Request (validation errors)
- 404: Resource Not Found
- 500: Internal Server Error

### 4. CORS Configuration

Configured to allow Flutter development:

- Allowed origins: `http://localhost:*`, `http://10.0.2.2:*`
- Allowed methods: GET, POST, PUT, DELETE, OPTIONS
- Credentials enabled

## Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `MONGODB_URI` | MongoDB connection string | Yes | - |
| `JWT_SECRET` | JWT signing secret (256+ bits) | Yes | Default dev key |
| `SERVER_PORT` | Application port | No | 8080 |
| `APP_SEED_DATA_ENABLED` | Enable database seeding | No | true |

## Security Notes

1. **Never commit** `.env` file or MongoDB credentials
2. **Change** JWT_SECRET in production
3. **Use** strong passwords for MongoDB
4. **Enable** MongoDB IP whitelist in production
5. Passwords are encrypted using BCrypt

## Troubleshooting

### MongoDB Connection Failed

```
Error: MongoSocketOpenException
```

**Solution:**
- Check MONGODB_URI is correct
- Verify network access (IP whitelist) in MongoDB Atlas
- Ensure database user has correct permissions

### Port Already in Use

```
Error: Port 8080 is already in use
```

**Solution:**
```bash
# Change port in application.properties
server.port=8081
```

### Build Failures

```bash
# Clean and rebuild
mvn clean install -U
```

## Production Deployment

1. Set environment variables properly
2. Disable database seeding
3. Use strong JWT secret
4. Enable HTTPS
5. Configure proper CORS origins
6. Set up logging and monitoring
7. Use connection pooling for MongoDB

## Next Steps

1. Add more REST endpoints for:
   - Events
   - HangOuts
   - Orders
   - Loyalty/Games
   - Notifications
2. Implement JWT token validation in controllers
3. Add pagination for large result sets
4. Implement caching (Redis)
5. Add API documentation (Swagger/OpenAPI)
6. Write unit and integration tests
7. Set up CI/CD pipeline

## Support

For issues or questions, check:
- Application logs
- MongoDB Atlas logs
- Network connectivity
- Environment variables

---

**Version:** 1.0.0  
**Last Updated:** Latest
