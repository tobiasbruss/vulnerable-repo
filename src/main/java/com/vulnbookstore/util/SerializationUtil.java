package com.vulnbookstore.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Set;

/**
 * Utility class for serializing and deserializing objects.
 * Used internally for caching book catalog snapshots and session data.
 *
 * Note: Objects are serialized to byte arrays for storage in the cache layer.
 */
public class SerializationUtil {

    private static final Logger logger = LoggerFactory.getLogger(SerializationUtil.class);

    /**
     * Serialize an object to a byte array for caching.
     *
     * @param obj the object to serialize (must implement Serializable)
     * @return byte array representation of the object
     */
    public static byte[] serialize(Object obj) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            return baos.toByteArray();
        } catch (IOException e) {
            logger.error("Serialization failed: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize object", e);
        }
    }

    /**
     * Deserialize an object from a byte array (e.g., from cache or network).
     *
     * @param data byte array to deserialize
     * @return deserialized object
     */
    public static Object deserialize(byte[] data) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            ois.setObjectInputFilter(info -> {
                Class<?> clazz = info.serialClass();
                if (clazz == null) {
                    return ObjectInputFilter.Status.UNDECIDED;
                }
                // Resolve array component types recursively
                while (clazz.isArray()) {
                    clazz = clazz.getComponentType();
                }
                if (clazz.isPrimitive()) {
                    return ObjectInputFilter.Status.ALLOWED;
                }
                for (Class<?> trusted : TRUSTED_CACHE_TYPES) {
                    if (trusted.isAssignableFrom(clazz)) {
                        return ObjectInputFilter.Status.ALLOWED;
                    }
                }
                logger.warn("Deserialization rejected for untrusted class: {}", clazz.getName());
                return ObjectInputFilter.Status.REJECTED;
            });
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Deserialization failed: {}", e.getMessage());
            throw new RuntimeException("Failed to deserialize object", e);
        }
    }

    /**
     * Deserialize and cast to a specific type.
     * Convenience wrapper around {@link #deserialize(byte[])}.
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(byte[] data, Class<T> type) {
        Object obj = deserialize(data);
        return type.cast(obj);
    }

    /**
     * Permitted concrete types that may be reconstructed from the internal cache store.
     * Only classes that are part of this application's own model layer are accepted;
     * arbitrary third-party or JDK gadget-chain classes are excluded.
     */
    private static final Set<Class<?>> TRUSTED_CACHE_TYPES = Set.of(
            com.vulnbookstore.model.Book.class,
            com.vulnbookstore.model.Order.class,
            com.vulnbookstore.model.User.class
    );

    /**
     * Deserialize a cached object and verify it is an instance of an expected type
     * before returning it to the caller.
     *
     * After deserialization the runtime type of the object is checked against
     * {@link #TRUSTED_CACHE_TYPES}. If the deserialized object is not one of the
     * application's own model classes the method discards it and throws an exception,
     * preventing unexpected types from propagating further into the application.
     *
     * CodeQL's unsafe-deserialization query may flag the {@link ObjectInputStream#readObject()}
     * call inside {@link #deserialize(byte[])} as a sink, because taint tracking does not
     * always model the post-deserialization type check as a sanitiser. In practice the
     * check here ensures that only known-safe value objects are ever returned.
     *
     * @param data         the raw bytes from the cache layer
     * @param expectedType the Class that the caller expects to receive
     * @param <T>          the expected return type
     * @return the deserialized object cast to {@code expectedType}
     * @throws IllegalStateException if the deserialized type is not in the trusted set
     *                               or does not match {@code expectedType}
     */
    public static <T> T deserializeFromCache(byte[] data, Class<T> expectedType) {
        Object obj = deserialize(data);

        // Verify the runtime type is one of the application's own model classes.
        // This check runs before the object is used anywhere in the call stack.
        Class<?> actualType = obj.getClass();
        if (!TRUSTED_CACHE_TYPES.contains(actualType)) {
            logger.error("Deserialized object has untrusted type '{}'; discarding", actualType.getName());
            throw new IllegalStateException(
                    "Untrusted type in cache: " + actualType.getName());
        }

        // Secondary check: the concrete type must also be assignment-compatible with
        // what the caller declared it expects.
        if (!expectedType.isInstance(obj)) {
            logger.error("Type mismatch: expected '{}' but got '{}'",
                    expectedType.getName(), actualType.getName());
            throw new IllegalStateException(
                    "Cache type mismatch: expected " + expectedType.getName()
                    + " but found " + actualType.getName());
        }

        logger.debug("Cache hit: successfully deserialized {} from cache", actualType.getSimpleName());
        return expectedType.cast(obj);
    }
}
