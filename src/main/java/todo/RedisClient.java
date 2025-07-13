package todo;
import redis.clients.jedis.Jedis;

public class RedisClient {
    private static Jedis jedis;

    static {
        String redisHost = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        jedis = new Jedis(redisHost, 6379);
    }

    public static void set(String key, String value) {
        jedis.set(key, value);
    }

    public static void setex(String key, int seconds, String value) {
        jedis.setex(key, seconds, value);
    }

    public static String get(String key) {
        return jedis.get(key);
    }

    public static void delete(String key) {
        jedis.del(key);
    }

    public static void deleteByPattern(String pattern) {
        for (String key : jedis.keys(pattern)) {
            jedis.del(key);
        }
    }
}

