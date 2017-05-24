package edu.ouc.mail.service.impl;

import com.google.common.collect.Lists;
import edu.ouc.mail.service.IFileService;
import edu.ouc.mail.util.DateTimeUtil;
import edu.ouc.mail.util.FTPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Created by tmw090906 on 2017/5/23.
 */
@Service("iFileService")
public class FileServiceImpl implements IFileService {

    private Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    @Override
    public String upload(MultipartFile file,String path,String currentUser){
        String filename = file.getOriginalFilename();
        //拓展名
        String fileExtensionName = filename.substring(filename.lastIndexOf(".")+1);
        String uploadFileName = DateTimeUtil.dateToStr(new Date(),"yyyy-MM-dd_HH_mm_ss") + "_" + currentUser + "." + fileExtensionName;
        logger.info("开始上传文件，上传文件的文件名：{},上传的路径：{},新文件名:{}",filename,path,uploadFileName);

        File fileDir = new File(path);
        if(!fileDir.exists()){
            fileDir.setWritable(true);
            fileDir.mkdirs();
        }
        File targetFile = new File(path,uploadFileName);

        try {
            //文件上传成功
            file.transferTo(targetFile);


            //已经上传到ftp服务器上
            FTPUtil.uploadFile(Lists.newArrayList(targetFile));

            //删除targetFile里的文件
            targetFile.delete();


        } catch (IOException e) {
            logger.error("上传文件异常",e);
            return null;
        }
        return targetFile.getName();
    }
}
