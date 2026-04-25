## What is inside

- Users and Bots can create posts.
- Comments support nested replies maximum 20 levels deep.
- Virality score tracks how hot a post is.
- Bot limit: only 100 bot replies per post allowed.
- Cooldown system: a bot cannot interact with same human user more than once in 10 minutes.
- Notifications are grouped so users do not get spammed by many bot interactions at once.

## Technology used

- Java 21 + Spring Boot 3
- PostgreSQL for permanent data
- Redis for counters temporary data and caching
- Docker Compose for local setup

## How to run

```bash
# Start Postgres and Redis
docker-compose up -d

# Build application. skip tests I did not write many
./mvnw clean package -DskipTests

# Start the server
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

API is available at http://localhost:8080.

## API Endpoints
### Create user (or bot same endpoint)
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "isPremium": true}'
```

### Create post by user
```bash
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{"authorId": 1, "authorType": "USER", "content": "Hello world"}'
```

### Like a post (adds 20 to virality)
```bash
curl -X POST "http://localhost:8080/api/posts/1/like?userId=1"
```

### Bot adds a comment (top level not reply)
```bash
curl -X POST http://localhost:8080/api/posts/1/comments \
  -H "Content-Type: application/json" \
  -d '{"authorId": 2, "authorType": "BOT", "content": "Nice post!", "parentCommentId": null}'
```

Other endpoints exist but not fully documented now. You can look into controller classes.

## Guardrails explanation
### 1. Virality score

Redis stores integer value for each post. Updates are instant

Bot reply: 1

Human like: 20

Human comment: 50

### 2. Limit of 100 bot replies per post
This was hard part. If many bots comment exactly same time race condition can happen. I used Redis atomic increment.

When a bot wants to comment:

INCR post:id:bot_count in Redis. Because Redis is single-threaded increments happen one by one.

If value is less than or equal to 100 comment allowed.

If value is greater than 100 we do DECR to rollback and return HTTP 429 Too Many Requests.

This way exactly 100 bots succeed others get rejected no race condition.

Java code simplified

```java
Long count = redisTemplate.opsForValue().increment("post:" + id + ":bot_count");
if (count > 100) {
    redisTemplate.opsForValue().decrement(key); // rollback
    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
}
```

### 3. Nesting depth limit
Comments have parentCommentId field. When new reply is added I walk chain of parent comments to count depth. If depth reaches 20 request is rejected. Not efficient maybe but works for now.

### 4. Cooldown between bot and human
A bot cannot interact with same human more than once in 10 minutes. I use Redis key with TTL

Key example: cooldown:bot_3:human_5

Use SETNX (set if not exists) with expire time of 10 minutes

If key already exists request is rejected because cooldown active.

```java
Boolean ok = redisTemplate.opsForValue()
    .setIfAbsent("cooldown:bot_" + botId + ":human_" + humanId, "1", Duration.ofMinutes(10));
if (!ok) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
```

## Thread Safety: How Atomic Locks Work

The guardrails use Redis for thread safety instead of Java locks. This is the key part of the assignment.

### Why Redis

Redis is single-threaded. All commands execute one by one in order. This means:
- If 200 bots send requests at exact same millisecond
- Their INCR commands line up in a queue
- Each executes one by one
- Result: exactly 100 succeed 100 fail

No race condition possible because Redis does not process commands in parallel.

### How the atomic counter works

```
Bot 1 request: INCR post:1:bot_count -> Redis returns 1 (allowed)
Bot 2 request: INCR post:1:bot_count -> Redis returns 2 (allowed)
...
Bot 100 request: INCR post:1:bot_count -> Redis returns 100 (allowed)
Bot 101 request: INCR post:1:bot_count -> Redis returns 101 (rejected)
Bot 102 request: INCR post:1:bot_count -> Redis returns 102 (rejected)
```

If we used a Java HashMap instead:
```
Thread 1: read count (99)
Thread 2: read count (99)
Thread 1: increment to 100
Thread 2: increment to 100
Result: 100 bot comments but 2 more got through. Race condition.
```

### Rollback mechanism

If count exceeds 100 we do DECR to undo:
```java
Long count = redisTemplate.opsForValue().increment(key);
if (count > 100) {
    redisTemplate.opsForValue().decrement(key); // rollback
    return false;
}
```

This rollback is also atomic because DECR is a single Redis command.

### Stateless application

All state lives in Redis:
- Bot counters
- Cooldown timers
- Notification queues
- Virality scores

No Java static variables. No synchronized blocks. Application can run on 10 servers behind load balancer and still enforce exact 100 bot limit because they all talk to same Redis.

## Notification batching
When bot interacts with user's post:

First interaction: notification sent immediately.

More interactions within 15 minutes: they wait in a Redis queue.

Every 5 minutes a scheduled task (cron) groups queued notifications per user and sends one batch message like Bot X and 97 other bots interacted with your posts.

This prevents notification flood.

## Testing the 100-bot limit
You can send many requests parallel to test if guardrail works.

Example with PowerShell (send 104 requests expect 100 success 4 fail)

```powershell
for ($i=2; $i -le 105; $i++) {
    curl -X POST http://localhost:8080/api/posts/1/comments `
      -H "Content-Type: application/json" `
      -d "{\"authorId\": $i, \"authorType\": \"BOT\", \"content\": \"Bot $i\"}"
}
```

After that, check Redis counter:

```bash
docker exec redis_cache redis-cli GET post:1:bot_count
# Should show "100"
If value is 100 atomic increment worked correctly. If not something wrong.
```