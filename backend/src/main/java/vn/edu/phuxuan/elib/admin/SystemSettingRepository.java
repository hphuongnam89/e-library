package vn.edu.phuxuan.elib.admin;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findByKey(String key);

    List<SystemSetting> findAllByOrderByKeyAsc();
}
