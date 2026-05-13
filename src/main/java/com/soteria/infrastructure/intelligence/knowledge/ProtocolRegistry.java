package com.soteria.infrastructure.intelligence.knowledge;

import com.soteria.core.domain.emergency.Protocol;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-safe registry that loads and caches emergency protocols from JSON files.
 * 
 * <p>It uses an {@code index.json} manifest file located in the target directory
 * to discover which JSON files to parse, enabling flexible additions of protocol
 * categories without code changes.</p>
 */
public class ProtocolRegistry {
    private static final Logger logger = Logger.getLogger(ProtocolRegistry.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String protocolsPath;
    private final List<Protocol> protocols = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Protocol> protocolMap = new ConcurrentHashMap<>();

    public ProtocolRegistry(String protocolsPath) {
        this.protocolsPath = protocolsPath;
    }

    /**
     * Clears the current cache and reads the manifest ({@code index.json}) from the configured
     * classpath directory, parsing all listed JSON files into the registry.
     */
    public void loadProtocols() {
        try {
            logger.log(Level.INFO, "Loading protocols from classpath: {0}", protocolsPath);
            protocols.clear();
            protocolMap.clear();
            List<String> files = listResourceFiles(protocolsPath);
            if (files.isEmpty()) {
                logger.log(Level.WARNING, "No protocols found in {0}", protocolsPath);
                return;
            }

            for (String file : files) {
                if (file.endsWith(".json")) {
                    loadCategoryFile(protocolsPath + (protocolsPath.endsWith("/") ? "" : "/") + file);
                }
            }
            logger.log(Level.INFO, "Successfully loaded {0} protocols.", protocols.size());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load protocols", e);
        }
    }

    private List<String> listResourceFiles(String path) throws java.io.IOException {
        String manifestPath = path + (path.endsWith("/") ? "" : "/") + "index.json";
        try (InputStream in = getResource(manifestPath)) {
            if (in == null) {
                logger.log(Level.WARNING, "Manifest not found at {0}", manifestPath);
                return new ArrayList<>();
            }
            return mapper.readValue(in, new TypeReference<List<String>>() {});
        }
    }

    private void loadCategoryFile(String fullPath) {
        try (InputStream in = getResource(fullPath)) {
            if (in == null) {
                logger.log(Level.WARNING, "Resource not found: {0}", fullPath);
                return;
            }
            List<Protocol> list = mapper.readValue(in, new TypeReference<List<Protocol>>() {});
            if (list != null) {
                for (Protocol p : list) {
                    if (p != null && p.getId() != null) {
                        protocols.add(p);
                        protocolMap.put(p.getId(), p);
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, e, () -> "Error parsing protocols in file: " + fullPath);
        }
    }

    private InputStream getResource(String path) {
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        InputStream is = getClass().getClassLoader().getResourceAsStream(normalized);
        if (is == null) {
            is = getClass().getResourceAsStream(path);
        }
        return is;
    }

    public List<Protocol> getProtocols() {
        return new ArrayList<>(protocols);
    }

    public Protocol getProtocolById(String id) {
        return protocolMap.get(id);
    }
}
