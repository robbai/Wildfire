from pathlib import Path

from rlbot.matchconfig.match_config import PlayerConfig, Team

from exercises.ball_rolling_to_goalie import *
from exercises.backboard_shot import *
from exercises.runaway_ball import *


wildfire_path, wildfire_test_path = ('../python/wildfire.cfg', '../python/wildfireTest.cfg')


def make_default_playlist():
    # Add the exercises
    exercises = [
        #(BallRollingToGoalie('Ball Rolling To Goalie'), 1)
        (BackboardShot('Backboard Shot'), 1)
        #(RunawayBall('Runaway Ball'), 1)
        #(RunawayBall('Ball Still', ball_still = True), 1)
    ]

    # Configure Wildfire(s)
    for exercise in exercises:
        if exercise[1] <= 1:
            exercise[0].match_config.player_configs = [
                PlayerConfig.bot_config(Path(wildfire_test_path), Team.BLUE)
            ]
        else:
            exercise[0].match_config.player_configs = [
                PlayerConfig.bot_config(Path(wildfire_test_path), Team.BLUE)
                ,PlayerConfig.bot_config(Path(wildfire_path), Team.ORANGE)
            ]

    return [exercise[0] for exercise in exercises]
