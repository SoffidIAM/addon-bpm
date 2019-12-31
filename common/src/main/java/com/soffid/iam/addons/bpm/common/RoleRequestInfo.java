package com.soffid.iam.addons.bpm.common;

import java.io.Serializable;
import java.util.Set;

import com.soffid.iam.api.RoleAccount;

import es.caib.seycon.ng.comu.SoDRisk;

public class RoleRequestInfo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6814385468391558715L;
	
	String userName;
	String userFullName;
	String applicationName;
	String applicationDescription;
	Long roleId;
	Long previousRoleId;
	String roleDescription;
	String previousRoleDescription;
	boolean approved;
	boolean denied;
	Long taskInstance;
	Set<String> owners;
	String ownersString;
	Long parentRole; 
	String comments;
	private SoDRisk sodRisk;
	RoleAccount roleAccount;
	

	public String getApplicationName() {
		return applicationName;
	}
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}
	public String getApplicationDescription() {
		return applicationDescription;
	}
	public void setApplicationDescription(String applicationDescription) {
		this.applicationDescription = applicationDescription;
	}
	public Long getRoleId() {
		return roleId;
	}
	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}
	public Long getPreviousRoleId() {
		return previousRoleId;
	}
	public void setPreviousRoleId(Long previousRoleId) {
		this.previousRoleId = previousRoleId;
	}
	public String getRoleDescription() {
		return roleDescription;
	}
	public void setRoleDescription(String roleDescription) {
		this.roleDescription = roleDescription;
	}
	public String getPreviousRoleDescription() {
		return previousRoleDescription;
	}
	public void setPreviousRoleDescription(String previousRoleDescription) {
		this.previousRoleDescription = previousRoleDescription;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getUserFullName() {
		return userFullName;
	}
	public void setUserFullName(String userFullName) {
		this.userFullName = userFullName;
	}
	public boolean isApproved() {
		return approved;
	}
	public void setApproved(boolean approved) {
		this.approved = approved;
	}
	public boolean isDenied() {
		return denied;
	}
	public void setDenied(boolean denied) {
		this.denied = denied;
	}
	public Long getTaskInstance() {
		return taskInstance;
	}
	public void setTaskInstance(Long taskInstance) {
		this.taskInstance = taskInstance;
	}
	public Set<String> getOwners() {
		return owners;
	}
	public void setOwners(Set<String> owners) {
		this.owners = owners;
	}
	public String getOwnersString() {
		return ownersString;
	}
	public void setOwnersString(String ownersString) {
		this.ownersString = ownersString;
	}
	public Long getParentRole() {
		return parentRole;
	}
	public void setParentRole(Long parentRole) {
		this.parentRole = parentRole;
	}
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	public void setSodRisk(SoDRisk sodRisk) {
		this.sodRisk = sodRisk;
	}
	public SoDRisk getSodRisk() {
		return sodRisk;
	}
	public RoleAccount getRoleAccount() {
		return roleAccount;
	}
	public void setRoleAccount(RoleAccount roleAccount) {
		this.roleAccount = roleAccount;
	}


}
