package com.akademiaplus.usecases;

import com.akademiaplus.interfaceadapters.TenantRepository;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TenantCreationUseCase {
    private final TenantRepository tenantRepository;
    private final HashingService hashingService;
    private final PiiNormalizer piiNormalizer;
    private final ModelMapper modelMapper;

}
