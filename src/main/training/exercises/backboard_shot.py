from dataclasses import dataclass
from math import pi, copysign

from rlbot.utils.game_state_util import *

from rlbottraining.common_exercises.common_base_exercises import StrikerExercise
from rlbottraining.rng import SeededRandomNumberGenerator as SeededRandom
from rlbottraining.common_graders.goal_grader import StrikerGrader, Grader


@dataclass
class BackboardShot(StrikerExercise):

    grader: Grader = StrikerGrader(timeout_seconds=7)

    def make_game_state(self, rng: SeededRandom) -> GameState:
        side = copysign(1, rng.n11())
        car_speed = rng.uniform(0, 2200)
        return GameState(
            ball=BallState(physics=Physics(
                location=Vector3(side * rng.uniform(1200, 1500), 2000, 92.75),
                velocity=Vector3(-side * rng.uniform(400, 600), rng.uniform(1500, 1600), rng.uniform(900, 1400)),
                angular_velocity=Vector3(0, 0, 0))),
            cars={
                0: CarState(
                    physics=Physics(
                        location=Vector3(rng.uniform(-1800, 1800), 5120 - max(car_speed * 2, rng.uniform(1100, 1700)), 0),
                        rotation=Rotator(0, pi / 2, 0),
                        velocity=Vector3(0, car_speed, 0),
                        angular_velocity=Vector3(0, 0, 0)),
                    jumped=False,
                    double_jumped=False,
                    boost_amount=rng.uniform(0, 100))
            },
            boosts={i: BoostState(0) for i in range(34)},
        )
