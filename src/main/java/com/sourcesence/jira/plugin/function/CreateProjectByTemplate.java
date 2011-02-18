/*
  Copyright 2009 Sourcesense http://www.sourcesense.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 
 */
package com.sourcesence.jira.plugin.function;


import java.util.List;
import java.util.Map;

import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;

import webwork.dispatcher.ActionResult;

import com.atlassian.core.action.ActionUtils;
import com.atlassian.core.ofbiz.CoreFactory;
import com.atlassian.core.user.UserUtils;
import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.action.ActionNames;
import com.atlassian.jira.action.project.ProjectCreate;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.context.manager.JiraContextTreeManager;
import com.atlassian.jira.issue.customfields.CustomFieldUtils;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.issue.fields.config.manager.FieldConfigSchemeManager;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutScheme;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenScheme;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager;
import com.opensymphony.module.propertyset.PropertySet;
import com.atlassian.jira.project.AssigneeTypes;
import com.atlassian.jira.scheme.SchemeManager;
import com.atlassian.jira.util.ofbiz.GenericValueUtils;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.workflow.WorkflowException;
import com.opensymphony.user.User;

import org.apache.log4j.Logger;


public class CreateProjectByTemplate extends AbstractJiraFunctionProvider{
	
	private static final Logger log = Logger.getLogger(CreateProjectByTemplate.class);
	
	private User leader = null;
	private String projectKey = "";
	private String projectName = "";
	private String projectURL = "";
	private String projectDescription = "";
	
	private Long notificationSchemeId= null;
	private Long permissionSchemeId =null;
	private Long issueSecuritySchemeId =null;
	private Long issueTypeSchemeId = null;
	private Long fieldLayoutSchemeId = null;
	private Long issueTypeScreenSchemeId = null;
	private Long workflowSchemeId = null;
	private Long projectCategoryId = null;
	
	private User remoteUser = null;
	
	private static final String PROJECT_NAME_CF_NAME= "Project Name";
	private static final String PROJECT_KEY_CF_NAME= "Project Key";
	private static final String NOTIFICATION_SCHEME_CF_NAME= "Notification Scheme ID";
	private static final String PERMISSION_SCHEME_CF_NAME= "Permission Scheme ID";
	private static final String ISSUE_SECURITY_SCHEME_CF_NAME= "Issue Security Scheme ID";
	private static final String ISSUE_TYPE_SCHEME_CF_NAME= "Issue Type Scheme ID";
	private static final String FIELD_LAYOUT_SCHEME_CF_NAME= "Field Layout Scheme ID";
	private static final String ISSUE_TYPE_SCREEN_SCHEME_CF_NAME= "Issue Type Screen Scheme ID";
	private static final String WORKFLOW_SCHEME_CF_NAME= "Workflow Scheme ID";
	private static final String PROJECT_CATEGORY_CF_NAME= "Project Category ID";
	
	
	private static final String LEADER_CF_NAME= "Leader";
	
	
	
	

	public void execute(Map transientVars, Map args, PropertySet ps)
			throws WorkflowException {
		
		
		MutableIssue issue = getIssue(transientVars);
		String remoteUserName = (String) transientVars.get("username");
		
		parseInput(issue,remoteUserName);		
		validateInput();
			
		
		GenericValue project = createProject();
		
        
        if(issueTypeSchemeId!=null){
        	setIssueTypeScheme(project,issueTypeSchemeId);
        }
        
        if(fieldLayoutSchemeId!=null){
        	setFieldLayoutScheme(project,fieldLayoutSchemeId);
        }
        
        
        if(issueTypeScreenSchemeId!=null){
        	setIssueTypeScreenScheme(project,issueTypeScreenSchemeId);
        }
        
        if(workflowSchemeId!=null){
        	setWorkflowScheme(project,workflowSchemeId);
        }
        if(projectCategoryId!=null){
        	setProjectCategory(project,projectCategoryId);
        }

        // refresh project cache
        // otherwise the project will not show on the dashboard
        ManagerFactory.getProjectManager().refresh();
		clearParmeters();
		
	}
	
	
	private void parseInput(MutableIssue issue, String remoteUserName) throws WorkflowException{
		CustomFieldManager customFieldManager = ComponentManager.getInstance().getCustomFieldManager();

		projectName = getCustomFieldValueAsString(customFieldManager,issue,PROJECT_NAME_CF_NAME,true);
		projectKey = getCustomFieldValueAsString(customFieldManager,issue,PROJECT_KEY_CF_NAME,true);
		leader = getCustomFieldValueAsUser(customFieldManager,issue,LEADER_CF_NAME,true);
		
		String notificationSchemeString = getCustomFieldValueAsString(customFieldManager,issue,NOTIFICATION_SCHEME_CF_NAME,false);
		String permissionSchemeString = getCustomFieldValueAsString(customFieldManager,issue,PERMISSION_SCHEME_CF_NAME,false);
		String issueSecuritySchemeString = getCustomFieldValueAsString(customFieldManager,issue,ISSUE_SECURITY_SCHEME_CF_NAME,true);
		String issueTypeSchemeString = getCustomFieldValueAsString(customFieldManager,issue,ISSUE_TYPE_SCHEME_CF_NAME,false);
		String fieldLayoutSchemeIdString = getCustomFieldValueAsString(customFieldManager,issue,FIELD_LAYOUT_SCHEME_CF_NAME,false);
		String issueTypeScreenSchemeIdString = getCustomFieldValueAsString(customFieldManager,issue,ISSUE_TYPE_SCREEN_SCHEME_CF_NAME,false);
		String workflowSchemeIdString = getCustomFieldValueAsString(customFieldManager,issue,WORKFLOW_SCHEME_CF_NAME,false);
		String projectCategoryIdString = getCustomFieldValueAsString(customFieldManager,issue,PROJECT_CATEGORY_CF_NAME,false);
		
		
		
		try{
			if(notificationSchemeString!=null && !notificationSchemeString.equals("")){
				notificationSchemeId = new Long(Long.parseLong(notificationSchemeString));
			}
			
		}catch(NumberFormatException e){
			clearParmeters();
			throw new WorkflowException("Format error: The value in the \""+NOTIFICATION_SCHEME_CF_NAME +"\" must be the notification scheme id");
		}
		
		
		
		try{
			if(permissionSchemeString!=null && !permissionSchemeString.equals("") ){
				permissionSchemeId= new Long(Long.parseLong(permissionSchemeString));
			}
			
		}catch(NumberFormatException e){
			clearParmeters();
			throw new WorkflowException("Format error: The value in the field \""+PERMISSION_SCHEME_CF_NAME +"\" must be the permission scheme id");
		} 
		
		
		try{
			if(issueSecuritySchemeString!=null && !issueSecuritySchemeString.equals("")){
				issueSecuritySchemeId= new Long(Long.parseLong(issueSecuritySchemeString));
			}
		
		}catch(NumberFormatException e){
			clearParmeters();
			throw new WorkflowException("Format error: The value in the \""+ISSUE_SECURITY_SCHEME_CF_NAME +"\" must be the issue security scheme id");
		}
		
		try{
			if(issueTypeSchemeString!=null && !issueTypeSchemeString.equals("")){
				issueTypeSchemeId= new Long(Long.parseLong(issueTypeSchemeString));
			}
		
		}catch(NumberFormatException e){
			clearParmeters();
			throw new WorkflowException("Format error: The value in the \""+ISSUE_TYPE_SCHEME_CF_NAME +"\" must be the issue type scheme id");
		}
		
		try{
			if(fieldLayoutSchemeIdString!=null && !fieldLayoutSchemeIdString.equals("")){
				fieldLayoutSchemeId= new Long(Long.parseLong(fieldLayoutSchemeIdString));
			}
		
		}catch(NumberFormatException e){
			clearParmeters();
			throw new WorkflowException("Format error: The value in the \""+FIELD_LAYOUT_SCHEME_CF_NAME +"\" must be the field layout scheme id");
		}
		
		try{
			if(issueTypeScreenSchemeIdString!=null && !issueTypeScreenSchemeIdString.equals("")){
				issueTypeScreenSchemeId= new Long(Long.parseLong(issueTypeScreenSchemeIdString));
			}
		
		}catch(NumberFormatException e){
			clearParmeters();
			throw new WorkflowException("Format error: The value in the \""+ISSUE_TYPE_SCREEN_SCHEME_CF_NAME +"\" must be the issue type screen scheme id");
		}
		
		try{
			if(workflowSchemeIdString!=null && !workflowSchemeIdString.equals("")){
				workflowSchemeId= new Long(Long.parseLong(workflowSchemeIdString));
			}
		
		}catch(NumberFormatException e){
			clearParmeters();
			throw new WorkflowException("Format error: The value in the \""+WORKFLOW_SCHEME_CF_NAME +"\" must be the workflow scheme id");
		}
		try{
			if(projectCategoryIdString!=null && !projectCategoryIdString.equals("")){
				projectCategoryId= new Long(Long.parseLong(projectCategoryIdString));
			}
		
		}catch(NumberFormatException e){
			clearParmeters();
			throw new WorkflowException("Format error: The value in the \""+PROJECT_CATEGORY_CF_NAME +"\" must be the project category id");
		}
	
	
		try{
	
			remoteUser = UserUtils.getUser(remoteUserName);			
		}catch(Exception e ){
			log.error(e);
			clearParmeters();
			throw new WorkflowException("Due to system error the project couldn't be created. Please contact the administrtor");
			
		}
		
	}
	
	
	
	
	private void validateInput() throws WorkflowException{
		
		if(projectName==null || projectName.equals("")){
			clearParmeters();
			throw new WorkflowException("The field \""+PROJECT_NAME_CF_NAME+"\" must contain the project name");
		}else{
			
			if(ManagerFactory.getProjectManager().getProjectObjByName(projectName)!=null){
				throw new WorkflowException("A project with project name \""+projectName+"\" already exists.");
			}
		}
		
		if(projectKey==null || projectKey.equals("")){
			clearParmeters();
			throw new WorkflowException("The field \""+PROJECT_KEY_CF_NAME+"\" must contain the project key");
		}else{
			
			if(ManagerFactory.getProjectManager().getProjectObjByKey(projectKey)!=null){
				throw new WorkflowException("A project with project key \""+projectKey+"\" already exists.");
			}
		}
			
		
		if(leader==null ){
			clearParmeters();
			throw new WorkflowException("You must select a valid user in the \""+LEADER_CF_NAME+"\" field ");
		}
		
		if(permissionSchemeId!=null){
			//Check if the permission scheme id is valid
			try{
				if(ManagerFactory.getPermissionSchemeManager().getScheme(permissionSchemeId)==null){
					throw new WorkflowException("The number in the field \""+PERMISSION_SCHEME_CF_NAME +"\" must be a valid permission scheme id");
				}
			}catch (GenericEntityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new WorkflowException(e);
			}
		}
		
	   if(notificationSchemeId!=null){
			// Checks if the notification scheme id is valid
			try{
			 if(ManagerFactory.getNotificationSchemeManager().getScheme(notificationSchemeId)==null){
				 throw new WorkflowException("The number in the field \""+NOTIFICATION_SCHEME_CF_NAME +"\" must be a valid notification scheme id");
			 }
			}catch (GenericEntityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new WorkflowException(e);
			}
	   }
		
		// Checks if the issue security scheme id is valid
		if(issueSecuritySchemeId!=null){
			//Issue security scheme is not required
			try{
			 if(ManagerFactory.getIssueSecuritySchemeManager().getScheme(issueSecuritySchemeId)==null){
				 throw new WorkflowException("The number in the field \""+ISSUE_SECURITY_SCHEME_CF_NAME +"\" must be a valid issue security scheme id");
			 }
			}catch (GenericEntityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new WorkflowException(e);
			}
		}
		
		
		
		// Checks if the issue type scheme id is valid
		if(issueTypeSchemeId!=null){
			//Issue type scheme is not required
			try{
			  FieldConfigSchemeManager fieldConfigSchemeManager = (FieldConfigSchemeManager) ComponentManager.getComponentInstanceOfType(FieldConfigSchemeManager.class);
			  fieldConfigSchemeManager.getFieldConfigScheme(issueTypeSchemeId);
			}catch(Exception e){
			
			  throw new WorkflowException("The number in the field \""+ISSUE_SECURITY_SCHEME_CF_NAME +"\" must be a valid issue type scheme id");
			}
		}
		
		// Checks if the field layout scheme id is valid
		if(fieldLayoutSchemeId!=null){
			//Field layout scheme is not required
			try{
				 FieldLayoutManager fieldLayoutManager = ComponentManager.getInstance().getFieldLayoutManager();
				 fieldLayoutManager.getFieldLayoutScheme(fieldLayoutSchemeId); 
			}catch(Exception e){
			
			  throw new WorkflowException("The number in the field \""+FIELD_LAYOUT_SCHEME_CF_NAME +"\" must be a valid field layout scheme id");
			}
		}
		
		// Checks if the issue type screen scheme id is valid
		if(issueTypeScreenSchemeId!=null){
			//Issue type screen scheme is not required
			try{
				 
				IssueTypeScreenSchemeManager issueTypeScreenSchemeManager = (IssueTypeScreenSchemeManager)ComponentManager.getComponentInstanceOfType(IssueTypeScreenSchemeManager.class); 
				issueTypeScreenSchemeManager.getIssueTypeScreenScheme(issueTypeScreenSchemeId);

				
			}catch(Exception e){
			
			  throw new WorkflowException("The number in the field \""+ISSUE_TYPE_SCREEN_SCHEME_CF_NAME +"\" must be a valid issue type screen scheme id");
			}
		}
		
		// Checks if the workflow screen scheme id is valid
		if(workflowSchemeId!=null){
			//Workflow scheme is not required
			GenericValue workflowScheme=null;
			try{
				SchemeManager schemeManager = ManagerFactory.getWorkflowSchemeManager();
				workflowScheme = schemeManager.getScheme(workflowSchemeId);
				
			}catch(Exception e ){
				throw new WorkflowException(e);
			}
			if(workflowScheme==null){
				throw new WorkflowException("The number in the field \""+WORKFLOW_SCHEME_CF_NAME +"\" must be a valid workflow screen scheme id");
			}
		}
		
		// Checks if the project category id is valid
		if(projectCategoryId!=null){
			//Project category is not required
			GenericValue projectCategory=null;
			try{
				projectCategory = ManagerFactory.getProjectManager().getProjectCategory(projectCategoryId);	   
				
			}catch(Exception e ){
				throw new WorkflowException(e);
			}
			if(projectCategory==null){
				throw new WorkflowException("The number in the field \""+PROJECT_CATEGORY_CF_NAME +"\" must be a valid project category id");
			}
		}
		
		
		
		
	}

	
	private GenericValue createProject() throws WorkflowException{

		GenericValue project;
        try
        {

        	
            Map fields = EasyMap.build("name", projectName, "key",projectKey.toUpperCase(), "lead", leader, "url", projectURL, "description", projectDescription);
        	
            // Set the default assignee to Project Lead
            fields.put("assigneetype", new Long( AssigneeTypes.PROJECT_LEAD));
            
            ActionResult aResult = CoreFactory.getActionDispatcher().execute(ActionNames.PROJECT_CREATE, EasyMap.build("remoteUser", remoteUser, "fields", fields, "notificationScheme", notificationSchemeId, "permissionScheme", permissionSchemeId, "issueSecurityScheme", issueSecuritySchemeId));
            ActionUtils.checkForErrors(aResult);

            project = ((ProjectCreate) aResult.getFirstAction()).getProject();
            
            
        }
        catch (Exception e)
        {
            log.error("Could not create project: "+e, e);
            
            clearParmeters();
            
            throw new WorkflowException(e);
        }
        
        return project;
	}
	
	

	
	private void clearParmeters(){
		leader=null;
		projectKey ="";
		projectName= "";
		projectDescription ="";
		projectURL = "";
		
		notificationSchemeId = null;
		permissionSchemeId = null;
		issueSecuritySchemeId = null;
		issueTypeSchemeId = null;
		fieldLayoutSchemeId = null;
		issueTypeScreenSchemeId = null;
		workflowSchemeId = null;
		projectCategoryId=null;
		
		remoteUser = null;
		
	}
	
	private String getCustomFieldValueAsString(CustomFieldManager customFieldManager, MutableIssue issue, String customFieldName, boolean isMandatory) throws WorkflowException{
		
		String ret = null;
		
		try{
			CustomField projectNameCustomField = customFieldManager.getCustomFieldObjectByName(customFieldName );

		
			// retrieves the custom field value object from the issue
			Object customFieldValue = issue.getCustomFieldValue( projectNameCustomField );
				
			if(customFieldValue instanceof String){
				ret = (String) customFieldValue;
			}


		}catch(Exception e){
			
			log.debug(e.getMessage());
			if(isMandatory){
				throw new WorkflowException("The custom field "+customFieldName+" is mandatory");
			}
			
		}
		
		return ret;
	}

	
	
	
   private User getCustomFieldValueAsUser(CustomFieldManager customFieldManager, MutableIssue issue, String customFieldName, boolean isMandatory)  throws WorkflowException{
		
		User ret = null;
		
		try{
			CustomField projectNameCustomField = customFieldManager.getCustomFieldObjectByName(customFieldName );
	
			// retrieves the custom field value object from the issue
			Object customFieldValue = issue.getCustomFieldValue( projectNameCustomField );
				
			if(customFieldValue instanceof User){
				ret = (User) customFieldValue;
			}
		
	   }catch(Exception e){
			
			log.debug(e.getMessage());
			if(isMandatory){
				throw new WorkflowException("The custom field "+customFieldName+" is mandatory");
			}
			
		}
		
		return ret;
	}

   
   private void setIssueTypeScheme(GenericValue project,Long issueTypeSchemeId){
	
	   FieldConfigSchemeManager fieldConfigSchemeManager = (FieldConfigSchemeManager) ComponentManager.getComponentInstanceOfType(FieldConfigSchemeManager.class);
	   IssueTypeSchemeManager issueTypeSchemeManager = (IssueTypeSchemeManager) ComponentManager.getComponentInstanceOfType(IssueTypeSchemeManager.class);
	   JiraContextTreeManager treeManager = (JiraContextTreeManager) ComponentManager.getComponentInstanceOfType(JiraContextTreeManager.class);
	   
	   FieldManager fieldManager = ComponentManager.getInstance().getFieldManager();
	   FieldConfigScheme issueTypeScheme = fieldConfigSchemeManager.getFieldConfigScheme(issueTypeSchemeId);
	   
	   // SET ISSUE TYPE SCHEME
	   if (issueTypeScheme != issueTypeSchemeManager.getDefaultIssueTypeScheme()) {
	       List projectList = issueTypeScheme.getAssociatedProjects();
	       projectList.add(project);
	       Long[] projectIds = GenericValueUtils.transformToLongIds (projectList);
	       log.debug ("projectIds: $projectIds");
	    
	       // Set the contexts
	       List contexts = CustomFieldUtils.buildJiraIssueContexts(false,
	                                                               null,
	                                                               projectIds,
	                                                               treeManager);
	    
	       fieldConfigSchemeManager.updateFieldConfigScheme(issueTypeScheme, contexts, fieldManager.getConfigurableField(IssueFieldConstants.ISSUE_TYPE));
	       
	       
	   }

   }
	
   
   private void setFieldLayoutScheme(GenericValue project,Long fieldLayoutSchemeId) throws WorkflowException{
	   
	   FieldLayoutManager fieldLayoutManager = ComponentManager.getInstance().getFieldLayoutManager();
	   
	   try{
		   FieldLayoutScheme oldFieldLayoutScheme = fieldLayoutManager.getFieldLayoutScheme(project);
	       if (oldFieldLayoutScheme != null){
	           fieldLayoutManager.removeSchemeAssociation(project, oldFieldLayoutScheme);
	       }
	       
	       FieldLayoutScheme newFieldLayoutScheme = fieldLayoutManager.getFieldLayoutScheme(fieldLayoutSchemeId);
	
	       fieldLayoutManager.addSchemeAssociation(project, newFieldLayoutScheme);
	   }catch(Exception e){
			
			  throw new WorkflowException(e);
			}
   }
   
   private void setIssueTypeScreenScheme(GenericValue project, Long issueTypeScreenSchemeId) throws WorkflowException{
	   
	   IssueTypeScreenSchemeManager issueTypeScreenSchemeManager = (IssueTypeScreenSchemeManager)ComponentManager.getComponentInstanceOfType(IssueTypeScreenSchemeManager.class); 
		
		   try{
			   IssueTypeScreenScheme issueTyepScreenScheme = issueTypeScreenSchemeManager.getIssueTypeScreenScheme(issueTypeScreenSchemeId);
		       if (issueTyepScreenScheme != null){
		    	   issueTypeScreenSchemeManager.addSchemeAssociation(project, issueTyepScreenScheme);
		       }
		       
		      
		   }catch(Exception e){
				
				  throw new WorkflowException(e);
		   }
   }
   
   
   private void setWorkflowScheme(GenericValue project,Long workflowSchemeId) throws WorkflowException{
	
	   
	   SchemeManager schemeManager = ManagerFactory.getWorkflowSchemeManager();
	   
	   try{
		   GenericValue workflowScheme = schemeManager.getScheme(workflowSchemeId);
		   schemeManager.addSchemeToProject(project, workflowScheme);
		   // Clear the active workflow name cache
		   ComponentManager.getInstance().getWorkflowSchemeManager().clearWorkflowCache();
	   }catch(Exception e){
			
			  throw new WorkflowException(e);
	   }
	   
	   
   }
   
   private void setProjectCategory(GenericValue project,Long projectCategoryId) throws WorkflowException{

	   try{
		   GenericValue projectCategory = ManagerFactory.getProjectManager().getProjectCategory(projectCategoryId);	   
		   ManagerFactory.getProjectManager().setProjectCategory(project, projectCategory);
	   }catch(Exception e){
			  throw new WorkflowException(e);
	   }
   }
   
   

}
