package com.koushikdutta.async.rtsp.server;

import com.koushikdutta.async.rtsp.action.RtspOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * 我们支持的options仅为最小实现
 * Created by Leaves Zhang on 2017/3/23.
 */
public class DefaultOptionsRequestCallback implements RtspServerRequestCallback {

    public List<String> getSupportActions() {
        List<String> actions = new ArrayList<>();
        actions.add(RtspOptions.METHOD);
        return actions;
    }

    @Override
    public void onRequest(AsyncRtspServerRequest request, AsyncRtspServerResponse response) {
        List<String> actions = getSupportActions();
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (String s : actions) {
            if (!isFirst) {
                sb.append(',');
            }
            sb.append(s);
            isFirst = false;
        }
        response.getHeaders().add("Public", sb.toString());
        response.writeHead();
    }
}
