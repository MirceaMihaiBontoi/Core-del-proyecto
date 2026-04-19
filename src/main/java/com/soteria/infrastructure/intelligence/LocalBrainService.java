package com.soteria.infrastructure.intelligence;

import de.kherud.llama.InferenceParameters;
import de.kherud.llama.LlamaModel;
import de.kherud.llama.ModelParameters;
import de.kherud.llama.args.MiroStat;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Local LLM inference service powered by llama.cpp (GGUF format).
 * Offline-first, CPU + GPU (Metal/Vulkan) depending on the native build.
 */
public class LocalBrainService implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(LocalBrainService.class.getName());

    private final Path modelFile;
    private LlamaModel model;

    public LocalBrainService(Path modelFile) {
        this.modelFile = modelFile;
        initializeModel();
    }

    private void initializeModel() {
        ModelParameters params = new ModelParameters()
                .setModel(modelFile.toString())
                .setCtxSize(2048)
                .setGpuLayers(-1);

        this.model = new LlamaModel(params);
        logger.info("LocalBrainService ready with GGUF: " + modelFile.getFileName());
    }

    /**
     * Generates a response using an English-core prompt that instructs the model
     * to reply in the user's target language.
     */
    public String generateResponse(String userInput, String context, String targetLanguage) {
        String systemInstruction = String.format(
                "You are SoterIA, a professional emergency medical assistant. " +
                        "The following context is from the official English knowledge base. " +
                        "Analyze the context and respond to the user in %s. " +
                        "Be concise, firm, and prioritize life-saving actions.\nContext: %s",
                targetLanguage, context
        );

        String prompt = String.format(
                "<|im_start|>system\n%s<|im_end|>\n<|im_start|>user\n%s<|im_end|>\n<|im_start|>assistant\n",
                systemInstruction, userInput
        );

        InferenceParameters infer = new InferenceParameters(prompt)
                .setTemperature(0.3f)
                .setTopP(0.9f)
                .setNPredict(256)
                .setMiroStat(MiroStat.V2)
                .setStopStrings("<|im_end|>");

        try {
            return model.complete(infer).trim();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Local inference failed", e);
            return targetLanguage.equalsIgnoreCase("Spanish")
                    ? "Error en el motor de IA local. Verifique los protocolos en el Dashboard."
                    : "Local AI engine error. Check protocols in the Dashboard.";
        }
    }

    @Override
    public void close() {
        if (model != null) {
            model.close();
        }
    }
}
