package org.ednovo.gooru.core.api.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

public class SearchQuery implements IndexableEntry,Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3159011800673163322L;

	
	private String queryUId;
	private String query;
	private String userIp;
	private User user;
	private long timeTokenInMillis;
	private long resultCount;
	private String searchType;
	private Date queryTime;
	
	private Set<SearchResult> searchResults;

	public String getQueryUId() {
		return queryUId;
	}

	public void setQueryUId(String queryUId) {
		this.queryUId = queryUId;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getUserIp() {
		return userIp;
	}

	public void setUserIp(String userIp) {
		this.userIp = userIp;
	}

	public long getTimeTokenInMillis() {
		return timeTokenInMillis;
	}

	public void setTimeTokenInMillis(long timeTokenInMillis) {
		this.timeTokenInMillis = timeTokenInMillis;
	}

	public String getSearchType() {
		return searchType;
	}

	public void setSearchType(String searchType) {
		this.searchType = searchType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ednovo.gooru.domain.model.IndexableEntry#getEntryId()
	 */
	@Override
	public String getEntryId() {
		return this.queryUId;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
	public long getResultCount() {
		return resultCount;
	}

	public void setResultCount(long resultCount) {
		this.resultCount = resultCount;
	}

	public Date getQueryTime() {
		return queryTime;
	}

	public void setQueryTime(Date queryTime) {
		this.queryTime = queryTime;
	}

	public Set<SearchResult> getSearchResults() {
		return searchResults;
	}

	public void setSearchResults(Set<SearchResult> searchResults) {
		this.searchResults = searchResults;
	}

}
