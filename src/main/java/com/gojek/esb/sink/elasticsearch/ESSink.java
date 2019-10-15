package com.gojek.esb.sink.elasticsearch;

import com.gojek.esb.consumer.EsbMessage;
import com.gojek.esb.exception.DeserializerException;
import com.gojek.esb.sink.Sink;
import com.gojek.esb.sink.elasticsearch.client.ESSinkClient;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class ESSink implements Sink {

    private ESRequestBuilder esRequestBuilder;
    private ESSinkClient esSinkClient;
    private String type;
    private String index;

    @Override
    public List<EsbMessage> pushMessage(List<EsbMessage> esbMessages) throws IOException, DeserializerException {
        esbMessages
                .stream()
                .map((message) -> esRequestBuilder.buildRequest(
                        index, type, message)
                )
                .forEach((request) -> esSinkClient.processRequest(request));
        return new ArrayList<>();
    }

    @Override
    public void close() throws IOException {
        esSinkClient.close();
    }
}