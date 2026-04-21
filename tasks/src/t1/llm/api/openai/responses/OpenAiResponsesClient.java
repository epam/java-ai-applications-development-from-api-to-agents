package t1.llm.api.openai.responses;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.ResponsesModel;
import commons.model.Message;
import commons.model.Role;
import t1.llm.api.openai.BaseOpenAiClient;

import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenAI Responses API client using the official OpenAI Java SDK.
 * <p>
 * The Responses API differs from Chat Completions: it uses {@code instructions} for the system
 * prompt and {@code input} for the conversation history. Compare with
 * {@link CustomOpenAiResponsesClient} which demonstrates the raw HTTP layer.
 */
public class OpenAiResponsesClient extends BaseOpenAiClient {

    private final OpenAIClient client;

    public OpenAiResponsesClient(String endpoint, String modelName,
                                  String apiKey, String systemPrompt) {
        super(endpoint, modelName, apiKey, systemPrompt);
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    @Override
    public Message response(List<Message> messages) {
        var params = buildParams(messages);
        var resp = client.responses().create(params);
        String content = resp.output().stream()
                .filter(com.openai.models.responses.ResponseOutputItem::isMessage)
                .findFirst()
                .map(item -> item.asMessage().content().stream()
                        .filter(com.openai.models.responses.ResponseOutputMessage.Content::isOutputText)
                        .findFirst()
                        .map(c -> c.asOutputText().text())
                        .orElse(""))
                .orElse("");
        System.out.println(content);
        return new Message(Role.ASSISTANT, content);
    }

    @Override
    public Message streamResponse(List<Message> messages) {
        var params = buildParams(messages);
        var sb = new StringBuilder();
        try (var stream = client.responses().createStreaming(params)) {
            stream.stream()
                    .filter(com.openai.models.responses.ResponseStreamEvent::isOutputTextDelta)
                    .forEach(event -> {
                        String delta = event.asOutputTextDelta().delta();
                        System.out.print(delta);
                        sb.append(delta);
                    });
        }
        System.out.println();
        return new Message(Role.ASSISTANT, sb.toString());
    }

    private ResponseCreateParams buildParams(List<Message> messages) {
        List<ResponseInputItem> inputItems = messages.stream()
                .map(m -> ResponseInputItem.ofEasyInputMessage(
                        EasyInputMessage.builder()
                                .role(EasyInputMessage.Role.of(m.role().getValue()))
                                .content(m.content())
                                .build()))
                .collect(Collectors.toList());
        return ResponseCreateParams.builder()
                .model(ResponsesModel.ofString(modelName))
                .instructions(systemPrompt)
                .inputOfResponse(inputItems)
                .build();
    }
}
