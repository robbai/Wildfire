from dataclasses import dataclass
from math import pi

from rlbot.utils.game_state_util import *

from rlbottraining.common_exercises.common_base_exercises import GoalieExercise
from rlbottraining.rng import SeededRandomNumberGenerator as SeededRandom


@dataclass
class BallRollingToGoalie(GoalieExercise):

    def make_game_state(self, rng: SeededRandom) -> GameState:
        return GameState(
            ball=BallState(physics=Physics(
                location=Vector3(rng.uniform(-840, 840), -1500, 92.75),
                velocity=Vector3(0, rng.uniform(-2000, -3000), rng.uniform(500, 600)),
                angular_velocity=Vector3(0, 0, 0))),
            cars={
                0: CarState(
                    physics=Physics(
                        location=Vector3(0, -5200, 0),
                        rotation=Rotator(0, pi / 2, 0),
                        velocity=Vector3(0, 0, 0),
                        angular_velocity=Vector3(0, 0, 0)),
                    jumped=False,
                    double_jumped=False,
                    boost_amount=0)
            },
            boosts={i: BoostState(0) for i in range(34)},
        )
