package com.huicang.wise.infrastructure.repository.tag;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductTagRepository extends JpaRepository<ProductTagJpaEntity, Long> {
    ProductTagJpaEntity findByRfid(String rfid);
    ProductTagJpaEntity findByNfcUid(String nfcUid);
    ProductTagJpaEntity findByBarcode(String barcode);
    List<ProductTagJpaEntity> findByStatus(String status);
}

