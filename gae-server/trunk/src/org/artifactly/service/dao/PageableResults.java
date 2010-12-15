package org.artifactly.service.dao;

import java.util.List;

public class PageableResults<T> {
	
	private List<T> results;
	private String offset;
	
	public PageableResults() {
		
	}
	
	public List<T> getResults() {
		return results;
	}
	
	public void setResults(List<T> results) {
		this.results = results;
	}
	
	public String getOffset() {
		return offset;
	}
	
	public void setOffset(String offset) {
		this.offset = offset;
	}
}
