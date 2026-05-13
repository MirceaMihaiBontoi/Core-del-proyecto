package com.soteria.ui;

import atlantafx.base.theme.PrimerDark;
import com.soteria.core.model.UserData;
import com.soteria.infrastructure.bootstrap.BootstrapService;
import com.soteria.infrastructure.persistence.ProfileRepository;
import com.soteria.ui.chat.ChatController;
import com.soteria.ui.onboarding.OnboardingController;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.soteria.infrastructure.intelligence.llm.LlamaNativeBootstrap;
import com.soteria.infrastructure.intelligence.system.SystemCapability;
import com.soteria.ui.i18n.UiLocales;

/**
 * JavaFX {@link Application} entry: Primer Dark theme, {@link BootstrapService#preInitialize()}, then either
 * {@link OnboardingController} (no complete profile) or chat pre-load for a returning user.
 *
 * <p>Listens on {@link BootstrapService#readyProperty()} to swap to {@link ChatController} when provisioning finishes.
 * Window close and {@link #stop()} call {@link BootstrapService#shutdown()}.</p>
 */
public class MainApp extends Application {

    private Stage primaryStage;
    private boolean chatScreenVisible;
    private final BootstrapService bootstrap = new BootstrapService();
    private final ProfileRepository profiles = new ProfileRepository();
    private static final Logger log = Logger.getLogger(MainApp.class.getName());

    private static final String MAIN_CSS = "/styles/main.css";

    /** Mobile-style viewport (~9:19.5). */
    private static final double MOBILE_WIDTH = 400;
    private static final double MOBILE_HEIGHT = 860;

    /**
     * Applies {@link PrimerDark}, runs {@link BootstrapService#preInitialize()}, registers {@code readyProperty}
     * navigation, and shows onboarding or begins chat session setup from {@link ProfileRepository#load()}.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        bootstrap.preInitialize();

        // Register global listener for system readiness.
        // It will trigger navigation to chat if the profile is complete.
        bootstrap.readyProperty().addListener((obs, wasReady, isReady) -> {
            if (Boolean.TRUE.equals(isReady)) {
                tryNavigateToChat();
            }
        });

        profiles.load().ifPresentOrElse(
                this::initializeSession,
                this::launchOnboardingQuietly);

        primaryStage.setTitle("SoterIA");
        primaryStage.setWidth(MOBILE_WIDTH);
        primaryStage.setHeight(MOBILE_HEIGHT);
        primaryStage.setMinWidth(360);
        primaryStage.setMinHeight(720);
        primaryStage.setOnCloseRequest(e -> bootstrap.shutdown());
        primaryStage.show();
    }

    /**
     * Routes to onboarding when {@link UserData#isComplete()} is false; otherwise shows chat and starts
     * {@link BootstrapService#startProvisioning} with stored model and language.
     */
    private void initializeSession(UserData profile) {
        if (!profile.isComplete()) {
            launchOnboardingQuietly();
            return;
        }

        try {
            showChatScreen(profile);

            SystemCapability.AIModelProfile profileType = parseModelProfile(profile.preferredModel());
            String lang = (profile.preferredLanguage() != null && !profile.preferredLanguage().isBlank())
                    ? profile.preferredLanguage()
                    : "English";

            bootstrap.startProvisioning(profileType, lang);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Session initialization failed", e);
        }
    }

    /**
     * Resolves persisted model name to {@link SystemCapability.AIModelProfile}, or recommended hardware default.
     */
    private SystemCapability.AIModelProfile parseModelProfile(String modelName) {
        SystemCapability capabilities = new SystemCapability();
        if (modelName == null || modelName.isBlank()) {
            return capabilities.getRecommendedProfile();
        }
        SystemCapability.AIModelProfile parsed = SystemCapability.parseStoredProfile(modelName);
        return parsed != null ? parsed : capabilities.getRecommendedProfile();
    }

    /** Swallows exceptions from {@link #showOnboarding()} so startup never aborts the JVM silently without a log line. */
    private void launchOnboardingQuietly() {
        try {
            showOnboarding();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to show onboarding", e);
        }
    }

    /**
     * Loads onboarding FXML with {@link Locale#getDefault()} bundles, wires {@link OnboardingController}, and replaces
     * the primary {@link Scene}.
     */
    private void showOnboarding() throws IOException {
        Locale locale = Locale.getDefault();
        bootstrap.localizationService().setLocale(locale);
        ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", locale);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/onboarding-view.fxml"));
        loader.setResources(bundle);
        Parent root = loader.load();
        OnboardingController controller = loader.getController();
        controller.init(bootstrap, profiles, this);

        Scene scene = new Scene(root, MOBILE_WIDTH, MOBILE_HEIGHT);
        scene.getStylesheets().add(getClass().getResource(MAIN_CSS).toExternalForm());
        primaryStage.setScene(scene);
        chatScreenVisible = false;

        // Navigation is now handled by the global readyProperty listener in start()
    }

    /**
     * Called by {@link OnboardingController} after the user finishes setup; re-enters the same navigation gate as
     * {@link BootstrapService#readyProperty()} so chat opens when bootstrap and profile are both ready.
     */
    public void completeOnboarding() {
        // Just a hint to check if we can navigate now
        tryNavigateToChat();
    }

    /**
     * If {@link BootstrapService#readyProperty()} is true and disk profile is complete, switches to chat on the FX thread
     * (idempotent when {@link #chatScreenVisible}).
     */
    private synchronized void tryNavigateToChat() {
        log.info("Attempting navigation to chat...");
        if (bootstrap.readyProperty().get()) {
            profiles.load().ifPresentOrElse(
                this::navigateToChatIfRequired,
                () -> log.warning("Bootstrap is ready but NO PROFILE found. Cannot navigate yet.")
            );
        } else {
            log.info("Bootstrap is not ready yet. Navigation deferred.");
        }
    }

    /** Invoked when bootstrap is ready and a profile row exists; defers UI swap to {@link Platform#runLater(Runnable)}. */
    private void navigateToChatIfRequired(UserData p) {
        if (!p.isComplete()) {
            log.warning("Bootstrap is ready but profile is INCOMPLETE. Cannot navigate yet.");
            return;
        }

        log.info(() -> "Profile is complete for user: " + p.fullName() + ". Swapping to chat screen.");
        Platform.runLater(() -> {
            try {
                if (chatScreenVisible) {
                    log.info("Already in chat screen, skipping.");
                    return;
                }
                showChatScreen(p);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to transition to chat", e);
            }
        });
    }

    /**
     * Loads chat FXML with locale from {@link UiLocales#fromPreferredLanguage(String)}, initializes {@link ChatController},
     * and sets the window title from localized {@code app.title}.
     *
     * @param profile complete user profile
     */
    void showChatScreen(UserData profile) throws IOException {
        Locale locale = UiLocales.fromPreferredLanguage(profile.preferredLanguage());
        bootstrap.localizationService().setLocale(locale);
        ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", locale);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat-view.fxml"));
        loader.setResources(bundle);
        Parent root = loader.load();
        ChatController controller = loader.getController();
        controller.init(profile, bootstrap, profiles);

        Scene scene = new Scene(root, MOBILE_WIDTH, MOBILE_HEIGHT);
        scene.getStylesheets().add(getClass().getResource(MAIN_CSS).toExternalForm());
        primaryStage.setScene(scene);
        String windowName = profile.fullName();
        if (windowName == null || windowName.isBlank() || UserData.INCOMPLETE_NAME.equals(windowName)) {
            windowName = bootstrap.localizationService().getMessage("ui.session.untitled");
        }
        primaryStage.setTitle(bootstrap.localizationService().formatMessage("app.title", windowName));
        chatScreenVisible = true;
    }

    @Override
    public void stop() {
        bootstrap.shutdown();
    }

    /**
     * Ensures llama JNI is on the library path when needed, then {@link Application#launch(String...)}.
     */
    public static void main(String[] args) {
        LlamaNativeBootstrap.applyIfNeeded();
        launch(args);
    }
}
