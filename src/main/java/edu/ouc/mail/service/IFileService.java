package edu.ouc.mail.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Created by tmw090906 on 2017/5/23.
 */
public interface IFileService {

    String upload(MultipartFile file,String path,String currentUser);
}
