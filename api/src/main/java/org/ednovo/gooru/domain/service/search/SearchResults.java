/////////////////////////////////////////////////////////////
// SearchResults.java
// gooru-api
// Created by Gooru on 2014
// Copyright (c) 2014 Gooru. All rights reserved.
// http://www.goorulearning.org/
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
/////////////////////////////////////////////////////////////
package org.ednovo.gooru.domain.service.search;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class SearchResults<T> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6430955873153488215L;
	private List<T> searchResults;
	private long totalHitCount;
	private long searchCount;
	private String userInput;
	private String searchType;
	private String category;
	private String title;

	Map<String, Object> searchInfo;

	private String queryUId;

	public SearchResults() {

	}

	public List<T> getSearchResults() {
		return searchResults;
	}

	public void setSearchResults(List<T> searchResults) {
		this.searchResults = searchResults;
	}

	public String getUserInput() {
		return userInput;
	}

	public void setUserInput(String userInput) {
		this.userInput = userInput;
	}

	public long getTotalHitCount() {
		return totalHitCount;
	}

	public void setTotalHitCount(long totalHitCount) {
		this.totalHitCount = totalHitCount;
	}

	public String getSearchType() {
		return searchType;
	}

	public void setSearchType(String searchType) {
		this.searchType = searchType;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getCategory() {
		return category;
	}

	public Map<String, Object> getSearchInfo() {
		return searchInfo;
	}

	public void setSearchInfo(Map<String, Object> searchInfo) {
		this.searchInfo = searchInfo;
	}

	public String getQueryUId() {
		return queryUId;
	}

	public void setQueryUId(String queryUId) {
		this.queryUId = queryUId;
	}

	/**
	 * @return the searchCount
	 */
	public long getSearchCount() {
		return searchCount;
	}

	/**
	 * @param searchCount
	 *            the searchCount to set
	 */
	public void setSearchCount(long searchCount) {
		this.searchCount = searchCount;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

}
