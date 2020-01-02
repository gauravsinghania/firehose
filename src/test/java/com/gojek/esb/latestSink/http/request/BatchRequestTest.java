package com.gojek.esb.latestSink.http.request;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import com.gojek.esb.consumer.EsbMessage;
import com.gojek.esb.exception.DeserializerException;
import com.gojek.esb.latestSink.http.request.body.JsonBody;
import com.gojek.esb.latestSink.http.request.header.BasicHeader;
import com.gojek.esb.latestSink.http.request.uri.BasicUri;

import org.apache.http.client.methods.HttpPut;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BatchRequestTest {

  @Mock
  BasicUri uri;

  @Mock
  BasicHeader header;

  @Mock
  JsonBody body;

  private EsbMessage esbMessage;
  private List<EsbMessage> esbMessages;

  @Before
  public void setup() throws URISyntaxException {
    esbMessage = new EsbMessage(new byte[] { 10, 20 }, new byte[] { 1, 2 }, "sample-topic", 0, 100);
    esbMessages = Collections.singletonList(esbMessage);
    when(uri.build()).thenReturn(new URI("http://dummy.com"));
  }

  @Test
  public void shouldPutJsonWrappedMessageInOneRequest() throws DeserializerException, URISyntaxException {

    BatchRequest batchRequest = new BatchRequest(uri, header, body);
    List<HttpPut> requests = batchRequest.build(esbMessages);

    assertEquals(1, requests.size());
  }

  @Test
  public void shouldDelegateRequestContentBuilding() throws DeserializerException, URISyntaxException {

    BatchRequest batchRequest = new BatchRequest(uri, header, body);
    batchRequest.build(esbMessages);

    verify(uri, times(2)).build();
    verify(header, times(2)).build();
    verify(body, times(2)).serialize(esbMessages);
  }
}