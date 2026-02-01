package com.dataforge.web.security;

import java.util.ArrayList;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  // 简单的内存用户存储，实际应该从数据库或其他存储中获取
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    // 这里使用硬编码的用户，实际应该从数据库中获取
    if ("admin".equals(username)) {
      return new User(
          "admin",
          "$2a$10$xRjvWJ3Q4Xq5Y6Z7A8B9C0D1E2F3G4H5I6J7K8L9M0N1O2P3Q4R5S6T7U8V9W0X1Y2Z3A4B", // 密码：admin123
          new ArrayList<>());
    } else {
      throw new UsernameNotFoundException("User not found with username: " + username);
    }
  }
}
