package com.soteria.infrastructure.bootstrap;

import com.soteria.core.port.Brain;
import com.soteria.core.port.KnowledgeBase;
import com.soteria.core.port.STT;
import com.soteria.core.port.TTS;
import com.soteria.core.port.Triage;
import com.soteria.infrastructure.intelligence.llm.LocalBrainService;
import com.soteria.infrastructure.intelligence.knowledge.EmergencyKnowledgeBase;
import com.soteria.infrastructure.intelligence.system.ModelManager;
import com.soteria.infrastructure.intelligence.system.SystemCapability;
import com.soteria.infrastructure.intelligence.triage.TriageService;
import com.soteria.infrastructure.intelligence.stt.SherpaSTTService;
import com.soteria.infrastructure.intelligence.tts.SherpaTTSService;
import com.soteria.infrastructure.intelligence.kws.WakeWordService;
import com.soteria.infrastructure.intelligence.system.ResourceLocalizationService;
import com.soteria.core.port.LocalizationService;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton facade that owns the two-phase startup of SoterIA.
 *
 * <p>{@link #preInitialize()} runs as soon as the application launches: it
 * detects hardware capabilities and builds the Lucene/JGraphT index so the
 * onboarding screen doubles as a loading screen. {@link #startProvisioning}
 * fires when the user confirms their profile and triggers model downloads plus
 * native-engine initialization.</p>
 *
 * <p>Exposes observable properties for direct FXML binding and a
 * {@link CompletableFuture} so {@code ChatController} can gate on readiness
 * before enabling the chat UI. All services are held as singletons; callers
 * must await {@link #ready()} before accessing them.</p>
 *
 * @see BootstrapState
 * @see ProvisioningManager
 */
public class BootstrapService {

    private static final Logger log = Logger.getLogger(BootstrapService.class.getName());

    private final BootstrapState state = new BootstrapState();
    private final ProvisioningManager provisioningManager = new ProvisioningManager();

    private static final String PROTOCOLS_PATH = System.getProperty("soteria.protocols.path", "/data/protocols/");

    private SystemCapability capability;
    private ModelManager modelManager;
    private EmergencyKnowledgeBase knowledgeBase;
    private SherpaSTTService sttService;
    private SherpaTTSService ttsService;
    private WakeWordService wakeWordService;
    private TriageService triageService;
    private LocalBrainService brainService;
    private LocalizationService localizationService;

    /**
     * Phase 1 of startup: hardware detection and local indexing.
     *
     * <p>Loads native libraries, detects system capabilities, and builds the
     * Lucene/JGraphT protocol index. Leaves {@link ModelManager} ready for
     * model-availability queries. Does <em>not</em> trigger any network
     * downloads.</p>
     *
     * <p>Must be called before {@link #startProvisioning}. On failure the
     * progress state reflects the error and {@link #ready()} never completes.</p>
     */
    public void preInitialize() {
        try {
            log.info("Starting pre-initialization...");

            // Configure native library paths before any native library usage
            com.soteria.infrastructure.intelligence.system.NativeLibraryLoader.load();
            log.info("Native library paths configured successfully");

            localizationService = new ResourceLocalizationService();
            state.update(localizationService.getMessage("onboarding.bootstrap.detecting_hardware"), 0.10);
            capability = new SystemCapability();
            modelManager = new ModelManager(capability);

            state.update(localizationService.getMessage("onboarding.bootstrap.building_knowledge"), 0.30);
            knowledgeBase = new EmergencyKnowledgeBase(PROTOCOLS_PATH, modelManager.getKBIndexPath(), capability);

            state.update(localizationService.getMessage("onboarding.bootstrap.system_ready_setup"), 1.0);
            log.info("Pre-initialization complete.");
        } catch (Exception e) {
            log.log(Level.SEVERE, "Pre-initialization failed", e);
            String err = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (localizationService != null) {
                state.update(localizationService.formatMessage("onboarding.bootstrap.init_error", err), 0.0);
            } else {
                state.update("Init Error: " + err, 0.0);
            }
        }
    }

    /**
     * Phase 2 of startup: model downloads and engine initialization.
     *
     * <p>Delegates to {@link ProvisioningManager}, which runs the full
     * provisioning sequence on a daemon thread. If called before
     * {@link #preInitialize()}, pre-initialization is triggered automatically.
     * Repeated calls with the same {@code profile} and {@code language} are
     * idempotent when provisioning is already in progress or complete.</p>
     *
     * @param profile LLM profile selected by the user during onboarding
     * @param language display-name of the selected language (e.g. {@code "Spanish"}, {@code "English"})
     */
    public void startProvisioning(SystemCapability.AIModelProfile profile, String language) {
        if (modelManager == null) {
            log.info("startProvisioning called before preInitialize. Triggering auto-init...");
            preInitialize();
        }
        log.info(() -> "BootstrapService: starting provisioning for " + profile + " in " + language);
        provisioningManager.start(state, this, profile, language);
    }

    /**
     * Returns the future that completes when all services are ready to handle
     * chat requests.
     *
     * <p>Completes exceptionally if provisioning fails. Replaced by a new
     * future whenever the user changes profile or language during onboarding.</p>
     *
     * @return synchronization future; never {@code null}
     */
    public CompletableFuture<Void> ready() {
        return state.getReadyFuture();
    }

    // Observable properties for FXML binding — contract is self-evident from name and type.

    public ReadOnlyStringProperty statusProperty() {
        return state.statusProperty();
    }

    public ReadOnlyBooleanProperty readyProperty() {
        return state.readyProperty();
    }

    public ReadOnlyDoubleProperty progressProperty() {
        return state.progressProperty();
    }

    // Service accessors (port interfaces) — only valid after ready() completes.

    public SystemCapability capability() {
        return capability;
    }

    public ModelManager modelManager() {
        return modelManager;
    }

    public KnowledgeBase knowledgeBase() {
        return knowledgeBase;
    }

    public STT sttService() {
        return sttService;
    }

    public TTS ttsService() {
        return ttsService;
    }

    public WakeWordService wakeWordService() {
        return wakeWordService;
    }

    public Triage triageService() {
        return triageService;
    }

    public Brain brainService() {
        return brainService;
    }

    public LocalizationService localizationService() {
        return localizationService;
    }

    // Package-private accessors for ProvisioningManager — expose concrete infra
    // types (centroid wiring, embedder injection, lifecycle) without leaking
    // them past the bootstrap package boundary.

    EmergencyKnowledgeBase knowledgeBaseImpl() {
        return knowledgeBase;
    }

    SherpaSTTService sttServiceImpl() {
        return sttService;
    }

    SherpaTTSService ttsServiceImpl() {
        return ttsService;
    }

    TriageService triageServiceImpl() {
        return triageService;
    }

    LocalBrainService brainServiceImpl() {
        return brainService;
    }

    // Package-private setters for ProvisioningManager.

    void setSttService(SherpaSTTService stt) {
        this.sttService = stt;
    }

    void setTtsService(SherpaTTSService tts) {
        this.ttsService = tts;
    }

    void setWakeWordService(WakeWordService wakeWordService) {
        this.wakeWordService = wakeWordService;
    }

    void setTriageService(TriageService triage) {
        this.triageService = triage;
    }

    void setBrainService(LocalBrainService brain) {
        this.brainService = brain;
    }

    /**
     * Stops all active services and terminates the process.
     *
     * <p>Calls {@link AutoCloseable#close()} on each service defensively —
     * individual failures are logged at FINE level and do not interrupt the
     * rest of the shutdown sequence. Ends with {@link System#exit(int)
     * System.exit(0)} because native ONNX and llama.cpp threads survive a
     * normal JVM shutdown and would keep the process alive indefinitely.</p>
     */
    public void shutdown() {
        log.info("System shutdown initiated. Cleaning up resources...");

        provisioningManager.shutdown();

        closeService(sttService, "STT");
        closeService(ttsService, "TTS");
        closeService(wakeWordService, "WakeWord");
        closeService(triageService, "Triage");
        closeService(brainService, "Brain");
        closeService(knowledgeBase, "KnowledgeBase");

        log.info("Cleanup complete.");

        // Force JVM exit — native threads (ONNX, llama.cpp) survive Java-level
        // shutdown and keep the process alive indefinitely without this.
        System.exit(0);
    }

    private void closeService(AutoCloseable service, String name) {
        try {
            if (service != null) {
                service.close();
            }
        } catch (Exception e) {
            log.log(Level.FINE, e, () -> "Cleanup of " + name + " failed (ignorable during shutdown)");
        }
    }
}
