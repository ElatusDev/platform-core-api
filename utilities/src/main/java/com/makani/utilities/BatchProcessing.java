package com.makani.utilities;

import java.util.List;

public interface BatchProcessing <D> {
     void createAll(List<D> dto);
}
