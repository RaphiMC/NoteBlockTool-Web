/*
 * This file is part of NoteBlockRenderer - https://github.com/RaphiMC/NoteBlockRenderer
 * Copyright (C) 2026-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.raphimc.noteblockrenderer.util;

import com.google.common.base.Joiner;
import org.teavm.jso.JSBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class HttpUtil {

    private HttpUtil() {
    }

    public static String withCorsProxy(final String url) {
        return withCorsProxy(url, null);
    }

    public static String withCorsProxy(final String url, final Map.Entry<String, String> header) {
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("key", getCorsProxyApiKey());
        queryParams.put("url", URLEncoder.encode(url, StandardCharsets.UTF_8));
        if (header != null) {
            queryParams.put("reqHeaders", URLEncoder.encode(header.getKey() + ':' + header.getValue(), StandardCharsets.UTF_8));
        }
        return "https://api.corsproxy.dev/proxy?" + Joiner.on('&').withKeyValueSeparator('=').join(queryParams);
    }

    @JSBody(script = "return __CORS_PROXY_API_KEY__;")
    private static native String getCorsProxyApiKey();

}
