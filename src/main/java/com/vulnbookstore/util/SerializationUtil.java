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
     * ⚠️ VULNERABILITY: Insecure Deserialization — ObjectInputStream is used
     * without any class filtering or allowlisting. An attacker who can control
     * the input byte array can supply a malicious serialized payload (e.g., using
     * gadget chains from commons-collections 3.2.1) to achieve Remote Code Execution.
     *
     * This is a subtle but critical vulnerability. The fix is to use a
     * ValidatingObjectInputStream with an explicit class allowlist, or switch
     * to a safer serialization format (JSON, Protobuf, etc.).
     *
     * CVE reference: CVE-2015-6420, CVE-2015-7501 (Apache Commons Collections)
     *
     * TODO: implement class filtering before deploying to production
     *
     * @param data byte array to deserialize
     * @return deserialized object
     */
    public static Object deserialize(byte[] data) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             // ⚠️ VULNERABILITY: raw ObjectInputStream — no class filtering
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
