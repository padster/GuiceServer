package com.github.padster.guiceserver.handlers;

import java.io.InputStream;

/** Types of responses the RouteHandler can format. */
public final class RouteHandlerResponses {
  /** Content of a text/plain result. */
  public static class TextResponse {
    public final String text;
    public TextResponse(String text) {
      this.text = text;
    }
  }

  /** Formatted content of an application/json result. */
  public static class JsonResponse {
    public final String json;
    public JsonResponse(String json) {
      this.json = json;
    }
  }

  /** Content that is streamed into the response. */
  public static class StreamResponse {
    public final long length;
    public final InputStream stream;
    public StreamResponse(long length, InputStream stream) {
      this.length = length;
      this.stream = stream;
    }
  }

  /** Result that needs to be processed by a Moustache template. */
  public static class MustacheResponse {
    public final String templateName;
    public final Object input;
    public MustacheResponse(String templateName, Object input) {
      this.templateName = templateName;
      this.input = input;
    }
  }

  /** CSV content to be presented as a downloaded file. */
  public static class CSVResponse {
    public final String fileName;
    public final String csvContent;
    public CSVResponse(String fileName, String csvContent) {
      this.fileName = fileName;
      this.csvContent = csvContent;
    }
  }

  /** File for download. */
  public static class FileDownloadResponse {
    public final String fileName;
    public final long length;
    public final String mimeType;
    public final InputStream stream;
    public FileDownloadResponse(String fileName, long length, String mimeType, InputStream stream) {
      this.fileName = fileName;
      this.length = length;
      this.mimeType = mimeType != null ? mimeType : "application/octet-stream";
      this.stream = stream;
    }
  }
}
