from dataclasses import dataclass, field
from typing import Any

from rlbot.utils.game_state_util import GameState, BoostState, BallState, CarState, Physics, Vector3, Rotator
from rlbottraining.common_graders.goal_grader import *
from rlbottraining.training_exercise import TrainingExercise
from rlbottraining.rng import SeededRandomNumberGenerator
from rlbottraining.training_exercise import Playlist


@dataclass
class bqzgsayptzdvfhtv(TrainingExercise):
	grader: Any = PassOnGoalForAllyTeam(ally_team=0)

	def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
		return GameState(
			ball=BallState(physics=Physics(
				location=Vector3(1437.9,4074.5999,290.43),
				velocity=Vector3(481.481,1407.231,-1633.341),
				angular_velocity=Vector3(5.54691,-1.9003099,-1.27281),
				rotation=Rotator(0.04343083,-0.69824886,-2.1147842))),
			cars={
				0: CarState(
					physics=Physics(
						location=Vector3(947.70996,810.54,17.05),
						velocity=Vector3(620.66095,959.18097,0.27099997),
						angular_velocity=Vector3(-4.0999998E-4,0.0,0.0),
						rotation=Rotator(-0.016682042,0.9965123,0.0)),
					jumped=False,
					double_jumped=False,
					boost_amount=0.0),
				1: CarState(
					physics=Physics(
						location=Vector3(-373.35,949.32996,43.309998),
						velocity=Vector3(115.690994,1242.251,340.411),
						angular_velocity=Vector3(-0.30971,0.02471,0.08900999),
						rotation=Rotator(-0.014668691,1.4870985,9.58738E-5)),
					jumped=True,
					double_jumped=False,
					boost_amount=0.0)
			},
			boosts={i: BoostState(0) for i in range(34)},
		)
