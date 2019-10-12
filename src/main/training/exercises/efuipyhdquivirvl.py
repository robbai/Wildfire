from dataclasses import dataclass, field
from typing import Any

from rlbot.utils.game_state_util import GameState, BoostState, BallState, CarState, Physics, Vector3, Rotator
from rlbottraining.common_graders.goal_grader import *
from rlbottraining.training_exercise import TrainingExercise
from rlbottraining.rng import SeededRandomNumberGenerator
from rlbottraining.training_exercise import Playlist


@dataclass
class efuipyhdquivirvl(TrainingExercise):
	grader: Any = PassOnGoalForAllyTeam(ally_team=0)

	def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
		return GameState(
			ball=BallState(physics=Physics(
				location=Vector3(-1611.2999,3176.77,1294.03),
				velocity=Vector3(-271.841,482.451,-253.021),
				angular_velocity=Vector3(-5.21131,-2.9428098,0.42601),
				rotation=Rotator(0.96103895,0.5027622,-1.1579638))),
			cars={
				0: CarState(
					physics=Physics(
						location=Vector3(-4056.2898,703.43,160.06),
						velocity=Vector3(-51.251,1661.851,150.251),
						angular_velocity=Vector3(-1.5182099,0.44171,-0.53651),
						rotation=Rotator(0.05119661,1.6048316,-1.1736871)),
					jumped=False,
					double_jumped=False,
					boost_amount=100.0),
				1: CarState(
					physics=Physics(
						location=Vector3(-875.76996,2313.48,16.279999),
						velocity=Vector3(529.34094,1679.131,12.241),
						angular_velocity=Vector3(0.02711,-4.0999998E-4,0.45961002),
						rotation=Rotator(-0.011217235,1.2832708,4.79369E-4)),
					jumped=False,
					double_jumped=False,
					boost_amount=0.0)
			},
			boosts={i: BoostState(0) for i in range(34)},
		)
