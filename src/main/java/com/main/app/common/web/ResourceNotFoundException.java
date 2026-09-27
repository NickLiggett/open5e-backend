package com.main.app.common.web;

/**
 * No resource with this key, or none the current user can see. Both give the same 404, so responses don't reveal
 * whether another user's content exists.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, String key) {
        super("No " + resource + " with key '" + key + "'");
    }
}
