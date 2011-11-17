Hello world


<%%

for(int i = 0; i < 10; ++i){
%>	test: <%=i %><%%
}
%>
<%+
int test(int i){
	return i;
}

%>
<%@ import java.net.URL; %>