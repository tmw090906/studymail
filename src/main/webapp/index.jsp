<%@ page language="java"  contentType="text/html; charset=UTF-8" %>

<html>
<head>
    <script src="ckeditor/ckeditor.js"></script>
</head>
<body>
<h2>Hello World!</h2>

    <form name="form1" enctype="multipart/form-data" action="manager/product/upload.do" method="post">
        <input type="file" name="upload_file">
        <input type="submit" value="SpringMVC上传单个文件测试">
    </form>

    <form>
        <textarea name="editor"></textarea>
        <script type="text/javascript">CKEDITOR.replace('editor')</script>
        <input type="submit" value="CKEditor测试">
    </form>


</body>
<script src="https://cdn.bootcss.com/jquery/1.12.4/jquery.min.js"></script>
<!-- 最新的 Bootstrap 核心 JavaScript 文件 -->
<script src="http://cdn.static.runoob.com/libs/bootstrap/3.3.7/js/bootstrap.min.js"></script>
<!-- 新 Bootstrap 核心 CSS 文件 -->
<link href="http://cdn.static.runoob.com/libs/bootstrap/3.3.7/css/bootstrap.min.css" rel="stylesheet">
</html>
