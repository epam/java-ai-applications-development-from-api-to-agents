package commons;

public final class Constants {

    private Constants() {}

    public static final String OPENAI_EMBEDDINGS_ENDPOINT =
            "https://api.openai.com/v1/embeddings";

    public static final String OPENAI_CHAT_COMPLETIONS_ENDPOINT =
            "https://api.openai.com/v1/chat/completions";

    public static final String OPENAI_RESPONSES_ENDPOINT =
            "https://api.openai.com/v1/responses";

    public static final String OPENAI_IMAGES_GENERATIONS_ENDPOINT =
            "https://api.openai.com/v1/images/generations";

    public static final String OPENAI_AUDIO_TRANSCRIPTIONS_ENDPOINT =
            "https://api.openai.com/v1/audio/transcriptions";

    public static final String OPENAI_AUDIO_SPEECH_ENDPOINT =
            "https://api.openai.com/v1/audio/speech";

    public static final String ANTHROPIC_ENDPOINT =
            "https://api.anthropic.com/v1/messages";

    public static final String GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models";

    public static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    public static final String ANTHROPIC_API_KEY = System.getenv("ANTHROPIC_API_KEY");
    public static final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY");

    public static final String DEFAULT_SYSTEM_PROMPT =
            "You are an assistant who answers concisely and informatively.";

    public static final String CLAUDE_SONNET_4_5 = "claude-sonnet-4-5";
    public static final String GEMINI_3_FLASH_PREVIEW = "gemini-3-flash-preview";
    public static final String GPT_5_4 = "gpt-5.4";

    public static final String WHISPER_1 = "whisper-1";
    public static final String GPT_4O_TRANSCRIBE = "gpt-4o-transcribe";
    public static final String GPT_4O_MINI_TTS = "gpt-4o-mini-tts";
    public static final String GPT_4O_AUDIO_PREVIEW = "gpt-4o-audio-preview";

}
