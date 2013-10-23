#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
<%@page language="java" contentType="text/html; charset=UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><%@ taglib uri="http://java.sun.com/portlet" prefix="portlet"
%>

<h2>Answer Equation</h2>

<c:choose>
  <c:when test="${symbol_dollar}{answerCorrect == true}">
    <p class="portlet-msg-success">Your answer was correct!</p>
  </c:when>
  <c:when test="${symbol_dollar}{answerCorrect == false}">
    <p class="portlet-msg-error">Your answer was incorrect!</p>
  </c:when>
</c:choose>

<p>Welcome, this is the example portlet in view mode. Please answer the following equation.</p>

<portlet:actionURL var="submitUrl">
    <portlet:param name="term1" value="${symbol_dollar}{term1}"/>
    <portlet:param name="term2" value="${symbol_dollar}{term2}"/>
    <portlet:param name="operator" value="${symbol_dollar}{operator}"/>
</portlet:actionURL>
<form method="post" action="${symbol_dollar}{submitUrl}">
<p>
    <c:out value="${symbol_dollar}{term1}"/>
    <c:out value="${symbol_dollar}{operator}"/>
    <c:out value="${symbol_dollar}{term2}"/>
    = <input type="text" name="answer" size="5" />
</p>
<p>
    <input type="submit" value="Answer" />
</p>
</form>

<p>You can also switch this portlet to
<a href="<portlet:renderURL portletMode="edit"/>">edit mode</a> or
<a href="<portlet:renderURL portletMode="help"/>">help mode</a>.</p>
