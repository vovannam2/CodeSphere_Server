package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.AccountEntity;
import com.hcmute.codesphere_server.model.entity.RoleEntity;
import com.hcmute.codesphere_server.model.entity.UserEntity;
import com.hcmute.codesphere_server.model.payload.request.LoginRequest;
import com.hcmute.codesphere_server.model.payload.request.RegisterRequest;
import com.hcmute.codesphere_server.model.payload.response.AuthResponse;
import com.hcmute.codesphere_server.repository.common.AccountRepository;
import com.hcmute.codesphere_server.repository.common.RoleRepository;
import com.hcmute.codesphere_server.repository.common.UserRepository;
import com.hcmute.codesphere_server.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Kiểm tra email đã tồn tại
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        // Tạo User
        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername() != null ? request.getUsername() : request.getEmail().split("@")[0]);
        user.setAvatar("https://res.cloudinary.com/dcti265mg/image/upload/v1728960991/453178253_471506465671661_2781666950760530985_n.png_ewlm3k.png");
        user.setStatus(true);
        user.setIsDeleted(false);
        user.setLastOnline(Instant.now());
        user = userRepository.save(user);

        // Lấy role mặc định
        RoleEntity role = roleRepository.findByName("ROLE_USER")
            .orElseGet(() -> {
                RoleEntity newRole = new RoleEntity();
                newRole.setName("ROLE_USER");
                return roleRepository.save(newRole);
            });

        // Tạo Account
        AccountEntity account = new AccountEntity();
        account.setUser(user);
        account.setEmail(request.getEmail());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setRole(role);
        account.setIsBlocked(false);
        account.setAuthenWith(0); // 0 = local
        account.setIsDeleted(false);
        account.setCreatedAt(Instant.now());
        account.setUpdatedAt(Instant.now());
        account = accountRepository.save(account);

        // Tạo token
        String token = jwtProvider.generateToken(
            user.getId().toString(),
            account.getEmail(),
            role.getName()
        );

        return AuthResponse.builder()
            .token(token)
            .type("Bearer")
            .userId(user.getId())
            .email(account.getEmail())
            .username(user.getUsername())
            .role(role.getName())
            .build();
    }

    public AuthResponse login(LoginRequest request) {
        // Xác thực
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
            )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Lấy account
        AccountEntity account = accountRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        UserEntity user = account.getUser();
        RoleEntity role = account.getRole();

        // Tạo token
        String token = jwtProvider.generateToken(
            user.getId().toString(),
            account.getEmail(),
            role.getName()
        );

        return AuthResponse.builder()
            .token(token)
            .type("Bearer")
            .userId(user.getId())
            .email(account.getEmail())
            .username(user.getUsername())
            .role(role.getName())
            .build();
    }
}

