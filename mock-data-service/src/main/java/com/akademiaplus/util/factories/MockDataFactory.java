package com.akademiaplus.util.factories;

import java.util.List;

public interface MockDataFactory<D> {

    public List<D> generate(int count);
}
