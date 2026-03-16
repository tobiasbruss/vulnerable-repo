package com.vulnbookstore.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

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
}
