package com.TestGeneration.service.model;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)
@Component
public class AnalysisResult {
	
    private String language;
	private String content;
    private String filename;
    private String codeSource;

    public String getCodeSource() {
		return codeSource;
	}

	public void setCodeSource(String codeSource) {
		this.codeSource = codeSource;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public AnalysisResult(String language, String content, String filename, String codeSource) {
		super();
		this.language = language;
		this.content = content;
		this.filename = filename;
		this.codeSource = codeSource;

	}


	public AnalysisResult() {

	}
}
