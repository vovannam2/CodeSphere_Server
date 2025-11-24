package com.hcmute.codesphere_server.repository.common;

import com.hcmute.codesphere_server.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

}
