package edu.cqu.core;

import edu.cqu.result.Result;

public interface ProgressChangedListener {
    void onProgressChanged(double percentage, Result result);
    void interrupted(String reason);
}
