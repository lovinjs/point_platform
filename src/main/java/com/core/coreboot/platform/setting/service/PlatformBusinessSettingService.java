package com.core.coreboot.platform.setting.service;

import com.core.coreboot.platform.setting.model.CurrentBusinessPolicy;
import com.core.coreboot.platform.setting.model.PlatformBusinessSettingUpdateCommand;
import com.core.coreboot.platform.setting.model.PlatformBusinessSettingView;

public interface PlatformBusinessSettingService {
    PlatformBusinessSettingView getCurrent(Long operatorId);

    PlatformBusinessSettingView update(PlatformBusinessSettingUpdateCommand command);

    CurrentBusinessPolicy currentPolicy();
}
