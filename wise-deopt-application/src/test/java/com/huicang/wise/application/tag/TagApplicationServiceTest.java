package com.huicang.wise.application.tag;

import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.infrastructure.repository.tag.ProductTagJpaEntity;
import com.huicang.wise.infrastructure.repository.tag.ProductTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TagApplicationServiceTest {

    @Mock
    private ProductTagRepository productTagRepository;

    @InjectMocks
    private TagApplicationService tagApplicationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getTagByNfcUid_Success() {
        String nfcUid = "NFC123";
        ProductTagJpaEntity entity = new ProductTagJpaEntity();
        entity.setNfcUid(nfcUid);
        entity.setRfid("RFID123");
        entity.setBarcode("BAR123");

        when(productTagRepository.findByNfcUid(nfcUid)).thenReturn(entity);

        ProductTagDTO result = tagApplicationService.getTagByNfcUid(nfcUid);

        assertNotNull(result);
        assertEquals(nfcUid, result.getNfcUid());
    }

    @Test
    void getTagByNfcUid_NotFound() {
        String nfcUid = "NON_EXISTENT";
        when(productTagRepository.findByNfcUid(nfcUid)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            tagApplicationService.getTagByNfcUid(nfcUid);
        });

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getTagByBarcode_Success() {
        String barcode = "BAR123";
        ProductTagJpaEntity entity = new ProductTagJpaEntity();
        entity.setBarcode(barcode);
        entity.setRfid("RFID123");
        entity.setNfcUid("NFC123");

        when(productTagRepository.findByBarcode(barcode)).thenReturn(entity);

        ProductTagDTO result = tagApplicationService.getTagByBarcode(barcode);

        assertNotNull(result);
        assertEquals(barcode, result.getBarcode());
    }

    @Test
    void getTagByBarcode_NotFound() {
        String barcode = "NON_EXISTENT";
        when(productTagRepository.findByBarcode(barcode)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            tagApplicationService.getTagByBarcode(barcode);
        });

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }
}
