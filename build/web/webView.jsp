<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Zappos - Restaurant</title>
    </head>

    <!--Check for the attribute which are present and accordingly display values associated with those attributes-->
    <% if (request.getAttribute("pictureURL") != null) {%>
    <%= request.getAttribute("pictureURL")%> 
    <br><hr>
    <% } %> 
    <% if (request.getAttribute("serverReply") != null) {%>
    <%= request.getAttribute("serverReply")%> 
    <br><hr>
    <% } %> 
    <% if (request.getAttribute("wineRecommendation") != null) {%>
    <%= request.getAttribute("wineRecommendation")%> 
    <br><hr>
    <% } %> 
    <% if (request.getAttribute("foodJoke") != null) {%>
    <%= request.getAttribute("foodJoke")%> 
    <br><hr>
    <% }%> 


</html>
