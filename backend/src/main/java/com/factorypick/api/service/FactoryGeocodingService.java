package com.factorypick.api.service;

import com.factorypick.api.domain.Factory;
import com.factorypick.api.exception.NotFoundException;
import com.factorypick.api.repository.FactoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FactoryGeocodingService {
    private final FactoryRepository factories;
    private final KakaoGeocodingClient kakao;
    public FactoryGeocodingService(FactoryRepository factories, KakaoGeocodingClient kakao) {
        this.factories = factories; this.kakao = kakao;
    }
    public Factory geocode(long id) {
        Factory factory = factories.findById(id).orElseThrow(() -> new NotFoundException("공장을 찾을 수 없습니다."));
        if (factory.latitude() != null && factory.longitude() != null) return factory;
        var result = kakao.lookup(factory.address());
        if (factories.saveGeocoding(id, factory.address(), result) == 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "주소 또는 좌표가 변경되었습니다. 다시 조회해 주세요.");
        return factories.findById(id).orElseThrow(() -> new NotFoundException("공장을 찾을 수 없습니다."));
    }
}
