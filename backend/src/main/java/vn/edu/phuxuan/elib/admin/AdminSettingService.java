package vn.edu.phuxuan.elib.admin;

import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.phuxuan.elib.admin.dto.SystemSettingDto;
import vn.edu.phuxuan.elib.identity.AppUser;

@Service
public class AdminSettingService {

    private final SystemSettingRepository settingRepository;
    private final AuditService auditService;

    public AdminSettingService(SystemSettingRepository settingRepository, AuditService auditService) {
        this.settingRepository = settingRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "systemSettingsAll")
    public List<SystemSettingDto> getAllSettings() {
        return settingRepository.findAllByOrderByKeyAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "systemSettings", key = "#key")
    public String getSettingValue(String key, String defaultValue) {
        return settingRepository.findByKey(key)
                .map(SystemSetting::getValue)
                .orElse(defaultValue);
    }

    @Transactional
    @CacheEvict(value = {"systemSettings", "systemSettingsAll"}, allEntries = true)
    public SystemSettingDto updateSetting(String key, String newValue, AppUser currentUser) {
        SystemSetting setting = settingRepository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cấu hình với khóa: " + key));

        setting.setValue(newValue);
        setting.setUpdatedBy(currentUser);
        SystemSetting saved = settingRepository.save(setting);

        auditService.log(
                currentUser,
                "SETTING_UPDATE",
                "SYSTEM_SETTING",
                key,
                "Cập nhật cấu hình " + key
        );

        return toDto(saved);
    }

    private SystemSettingDto toDto(SystemSetting s) {
        String displayValue = s.isSecret() ? "********" : s.getValue();
        String updatedByEmail = s.getUpdatedBy() != null ? s.getUpdatedBy().getEmail() : null;
        return new SystemSettingDto(
                s.getId(),
                s.getKey(),
                displayValue,
                s.getDescription(),
                s.isSecret(),
                s.getUpdatedAt(),
                updatedByEmail
        );
    }
}
