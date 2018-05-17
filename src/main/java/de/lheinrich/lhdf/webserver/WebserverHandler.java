package de.lheinrich.lhdf.webserver;

import java.util.Map;
import java.util.TreeMap;

/*
 * Copyright (c) 2018 Lennart Heinrich
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

public abstract class WebserverHandler {

    private final Map<String, String> setCookies = new TreeMap<>();
    private final String contentType;

    /**
     * Initialize with Content-Type "text/plain"
     */
    public WebserverHandler() {
        this.contentType = "text/plain";
    }

    /**
     * Initialize with custom Content-Type
     *
     * @param contentType HTML Content-Type String
     */
    public WebserverHandler(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Get HTML Content-Type
     *
     * @return HTML Content-Type String
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Set a Cookie
     *
     * @param name  Name of the cookie
     * @param value Value of the cookie
     */
    public void setCookie(String name, String value) {
        this.setCookies.remove(name);
        this.setCookies.put(name, value);
    }

    /**
     * Get the Cookie Map
     *
     * @return TreeMap Name and value of the cookies
     */
    public Map<String, String> getCookies() {
        return this.setCookies;
    }

    /**
     * Task handling
     *
     * @param get      HTML GET Header
     * @param head     HTML HEADER
     * @param post_put HTML POST or PUT Header (default is PUT)
     * @param cookies  Cookies sent from client
     * @param clientIp IP Request is from
     * @return String for Output
     */
    public abstract String process(Map<String, String> get, Map<String, String> head, Map<String, String> post_put, Map<String, String> cookies, String clientIp);
}
